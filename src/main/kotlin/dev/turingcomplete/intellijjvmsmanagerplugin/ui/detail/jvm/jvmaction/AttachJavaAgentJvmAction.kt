package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction

import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjvmsmanagerplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.CommonsDataKeys
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.copyable
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.overrideLeftInset
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.overrideTopInset
import org.apache.commons.lang.WordUtils
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Supplier
import javax.swing.*
import javax.swing.border.EmptyBorder

class AttachJavaAgentJvmAction : JvmAction("Attach Java Agent") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val agentPathField = TextFieldWithBrowseButton()
  private val optionsField = JBTextField()
  private lateinit var agentPathFieldValidator: ComponentValidator

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent(project: Project, parent: Disposable) = JPanel(GridBagLayout()).apply {
    border = EmptyBorder(0, 0, 0, 0)

    val bag = UiUtils.createDefaultGridBag()

    add(JBLabel("Agent path:"), bag.nextLine().next().anchor(GridBagConstraints.WEST))

    val fileDescriptor = FileChooserDescriptor(true, false, true, true, false, false)
            .withFileFilter { file -> Comparing.equal(file.extension, "jar", SystemInfo.isFileSystemCaseSensitive) }
    agentPathField.addBrowseFolderListener("Java Agent File", "Select the Java agent JAR file.", project, fileDescriptor)
    add(agentPathField, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally())
    agentPathFieldValidator = createAgentPathFieldValidator(parent)

    add(JBLabel("Options:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP).anchor(GridBagConstraints.WEST))
    add(optionsField, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideTopInset(UIUtil.DEFAULT_VGAP).weightx(1.0).fillCellHorizontally())

    add(JButton(createAttachJavaAgentAction(agentPathField)), bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP))

    add(UiUtils.createCommentLabel("<html><a href=\"https://docs.oracle.com/en/java/javase/17/docs/api/java.instrument/java/lang/instrument/package-summary.html\">Starting an Agent After VM Startup</a></html>").apply {
      copyable()
    }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP))
  }

  private fun createAgentPathFieldValidator(parent: Disposable) = ComponentValidator(parent).withValidator(Supplier {
    when {
      agentPathField.text.isBlank() -> {
        ValidationInfo("The path must be provided.", agentPathField.textField)
      }

      !Files.isRegularFile(Path.of(agentPathField.text)) -> {
        ValidationInfo("The path is not a file or does not exist.", agentPathField.textField)
      }

      else -> {
        null
      }
    }
  }).andRegisterOnDocumentListener(agentPathField.textField).installOn(agentPathField.textField)

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createAttachJavaAgentAction(parent: JComponent): Action {
    return object : AbstractAction("Attach", AllIcons.RunConfigurations.TestState.Run) {
      override fun actionPerformed(e: ActionEvent) {
        val jvmActionContext = CommonsDataKeys.getRequiredData(JvmActionContext.DATA_KEY, DataManager.getInstance().getDataContext(parent))

        agentPathFieldValidator.revalidate()
        agentPathFieldValidator.validationInfo?.let { return }

        val agentPath = agentPathField.text
        val options = optionsField.text?.takeIf { it.isNotBlank() }

        parent.isEnabled = false
        AttachAgentTask(jvmActionContext, agentPath, options) { parent.isEnabled = true }.queue()
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class AttachAgentTask(private val jvmActionContext: JvmActionContext,
                        private val agentPath: String,
                        private val options: String?,
                        private val onFinished: () -> Unit)
    : Task.ConditionalModal(jvmActionContext.project, "Attach Java agent", false, DEAF) {

    companion object {
      private val LOG = Logger.getInstance(AttachAgentTask::class.java)
    }

    override fun run(indicator: ProgressIndicator) {
      LOG.info("Attaching Java agent '$agentPath' ${if (options != null) " with options $options" else ""} to process with PID ${jvmActionContext.processNode.process.processID}...")
      (jvmActionContext.processNode as JvmProcessNode).attachAgent(agentPath, options)
    }

    override fun onFinished() {
      onFinished.invoke()
    }

    override fun onThrowable(error: Throwable) {
      val errorMessage = "Attaching Java agent failed: ${error.message}"
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(project,
                               "$errorMessage\n\n" +
                               "See idea.log for more details.",
                               "${WordUtils.capitalize(title)} Failed")
    }
  }
}