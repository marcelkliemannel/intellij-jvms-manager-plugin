package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.*
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjvmsmanagerplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.CommonsDataKeys
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.*
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier
import javax.swing.*

class AttachJavaAgentJvmAction : JvmAction("Attach Java Agent") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var agentReferenceSelection: ComboBox<AgentReference>
  private val agentReferenceLabel = JBLabel()
  private val agentReferenceHelpLabel = UiUtils.createCommentLabel("").apply { copyable() }
  private val agentReferenceWrapper = BorderLayoutPanel()
  private val optionsField = JBTextField()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent(project: Project, parent: Disposable) = JPanel(GridBagLayout()).apply {
    border = UiUtils.EMPTY_BORDER

    val bag = UiUtils.createDefaultGridBag()

    val agents = listOf(InstrumentationAgentReference(project, parent),
                        LibraryPathAgentReference(project, parent),
                        LibraryBuiltInAgentReference(parent))
    agentReferenceSelection = ComboBox(CollectionComboBoxModel(agents))
    agentReferenceSelection.addActionListener { updateAgentReference() }
    add(JBLabel("Agent type:"), bag.nextLine().next().anchor(GridBagConstraints.WEST))
    add(agentReferenceSelection, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2))
    updateAgentReference()

    add(agentReferenceLabel, bag.nextLine().next().anchor(GridBagConstraints.WEST).overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(agentReferenceWrapper, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(agentReferenceHelpLabel, bag.nextLine().next().next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally().overrideTopInset(UIUtil.DEFAULT_VGAP / 2))

    add(JBLabel("Options:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP).anchor(GridBagConstraints.WEST))
    add(optionsField, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideTopInset(UIUtil.DEFAULT_VGAP).weightx(1.0).fillCellHorizontally())

    add(JButton().apply {
      action = createAttachJavaAgentAction(this)
    }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP))
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun updateAgentReference() {
    val selectedAgentType = getSelectedAgentType()
    agentReferenceLabel.text = "${selectedAgentType.agentReferenceName}:"
    agentReferenceWrapper.removeAll()
    agentReferenceWrapper.addToCenter(selectedAgentType.agentReferenceField)
    agentReferenceHelpLabel.text = "<html><a href=\"${selectedAgentType.agentReferenceHelpLink}\">${selectedAgentType.agentReferenceHelpTitle}</a></html>"
  }

  private fun getSelectedAgentType(): AgentReference = agentReferenceSelection.selectedItem as AgentReference

  private fun createAttachJavaAgentAction(parentComponent: JComponent): Action {
    return object : AbstractAction("Attach", AllIcons.RunConfigurations.TestState.Run) {
      override fun actionPerformed(e: ActionEvent) {
        val jvmActionContext = CommonsDataKeys.getRequiredData(JvmActionContext.DATA_KEY, DataManager.getInstance().getDataContext(parentComponent))
        val selectedAgentType = getSelectedAgentType()

        // Validate agent reference
        selectedAgentType.agentReferenceFieldValidator.revalidate()
        if (selectedAgentType.agentReferenceFieldValidator.validationInfo != null) {
          return
        }

        val options = optionsField.text?.takeIf { it.isNotBlank() }

        parentComponent.isEnabled = false
        AttachAgentTask(jvmActionContext, selectedAgentType, options) { parentComponent.isEnabled = true }.queue()
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class AttachAgentTask(private val jvmActionContext: JvmActionContext,
                                private val agentReference: AgentReference,
                                private val options: String?,
                                private val onFinished: () -> Unit)
    : Task.ConditionalModal(jvmActionContext.project, "Attaching Java agent", false, DEAF) {

    companion object {
      private val LOG = Logger.getInstance(AttachAgentTask::class.java)
    }

    override fun run(indicator: ProgressIndicator) {
      val agentReferenceValue = agentReference.agentReferenceField.text
      LOG.info("Attaching Java agent '$agentReference' (${this.agentReference.name}) ${if (options != null) " with options $options" else ""} " +
               "to process with PID ${jvmActionContext.processNode.process.processID}...")
      (jvmActionContext.processNode as JvmProcessNode).attachJavaAgent(agentReference.javaAgentType, agentReferenceValue, options)
    }

    override fun onFinished() {
      onFinished.invoke()
    }

    override fun onSuccess() {
      NotificationUtils.notifyOnToolWindow("Java agent attached to process with PID ${jvmActionContext.processNode.process.processID}.", project)
    }

    override fun onThrowable(error: Throwable) {
      val errorMessage = "Attaching Java agent failed: ${error.message}"
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(project,
                               "$errorMessage\n\n" +
                               "See idea.log for more details.",
                               "${StringUtil.capitalize(title)} Failed")
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private abstract class AgentReference(val name: String,
                                        val javaAgentType: JvmProcessNode.JavaAgentType,
                                        val agentReferenceName: String,
                                        val agentReferenceHelpTitle: String,
                                        val agentReferenceHelpLink: String,
                                        agentReferenceFieldAndValidator: Pair<JTextField, ComponentValidator>) {

    val agentReferenceField: JTextField
    val agentReferenceFieldValidator: ComponentValidator

    init {
      agentReferenceField = agentReferenceFieldAndValidator.first
      agentReferenceFieldValidator = agentReferenceFieldAndValidator.second
    }

    companion object {

      fun createAgentPathFieldValidator(field: JTextField, parent: Disposable): ComponentValidator {
        return ComponentValidator(parent).withValidator(Supplier {
          when {
            field.text.isBlank() -> {
              ValidationInfo("The path must be provided.", field)
            }

            !Files.isRegularFile(Path.of(field.text)) -> {
              ValidationInfo("The path is not a file or does not exist.", field)
            }

            else -> {
              null
            }
          }
        }).andRegisterOnDocumentListener(field).installOn(field)
      }
    }

    override fun toString(): String = name
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class InstrumentationAgentReference(project: Project, parent: Disposable)
    : AgentReference("Instrumentation Agent",
                     JvmProcessNode.JavaAgentType.INSTRUMENTATION,
                     "JAR file",
                     "Starting an Agent After VM Startup",
                     "https://docs.oracle.com/en/java/javase/17/docs/api/java.instrument/java/lang/instrument/package-summary.html",
                     createAgentJarPathField(project, parent)) {

    companion object {

      fun createAgentJarPathField(project: Project, parent: Disposable): Pair<JTextField, ComponentValidator> {
        val agentPathField = TextFieldWithBrowseButton()
        val fileDescriptor = FileChooserDescriptor(true, false, true, true, false, false)
                .withFileFilter { file -> Comparing.equal(file.extension, "jar", SystemInfo.isFileSystemCaseSensitive) }
        agentPathField.addBrowseFolderListener("Java Agent File", "Select the Java agent JAR file.", project, fileDescriptor)

        val agentPathFieldValidator = createAgentPathFieldValidator(agentPathField.textField, parent)

        return Pair(agentPathField.textField, agentPathFieldValidator)
      }
    }
  }

  private class LibraryPathAgentReference(project: Project, parent: Disposable)
    : AgentReference("Library (File)",
                     JvmProcessNode.JavaAgentType.LIBRARY_BUILT_IN,
                     "Library file",
                     "JVM Tool Interface",
                     "https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html",
                     createAgentLibraryPathField(project, parent)) {

    companion object {

      fun createAgentLibraryPathField(project: Project, parent: Disposable): Pair<JTextField, ComponentValidator> {
        val agentPathField = TextFieldWithBrowseButton()
        val fileDescriptor = FileChooserDescriptor(true, false, true, true, false, false)
        agentPathField.addBrowseFolderListener("Java Agent Library File", "Select the Java agent library file.", project, fileDescriptor)

        val agentPathFieldValidator = createAgentPathFieldValidator(agentPathField.textField, parent)

        return Pair(agentPathField.textField, agentPathFieldValidator)
      }
    }
  }

  private class LibraryBuiltInAgentReference(parent: Disposable)
    : AgentReference("Library (Built-In)",
                     JvmProcessNode.JavaAgentType.LIBRARY_BUILT_IN,
                     "Library name",
                     "JVM Tool Interface",
                     "https://docs.oracle.com/javase/8/docs/platform/jvmti/jvmti.html",
                     createAgentLibraryPathField(parent)) {

    companion object {

      fun createAgentLibraryPathField(parent: Disposable): Pair<JTextField, ComponentValidator> {
        val agentNameField = JBTextField()
        val agentNameFieldValidator = ComponentValidator(parent).withValidator(Supplier {
          if (agentNameField.text.isBlank()) {
            ValidationInfo("The name must be provided.", agentNameField)
          }
          else {
            null
          }
        }).andRegisterOnDocumentListener(agentNameField).installOn(agentNameField)

        return Pair(agentNameField, agentNameFieldValidator)
      }
    }
  }
}