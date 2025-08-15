package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.jtool

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.ListLayout
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.*
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.UiUtils.createLink
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmAction
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmActionContext
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class GetJvmInformationAction : JvmAction("Get JVM Information") {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    private val COMMANDS =
      arrayOf(
        Command("VM.version", "Version", "Provides JVM version information."),
        Command(
          "VM.info",
          "Environment and Status",
          "Provides information about the JVM environment and status.",
        ),
        Command(
          "VM.uptime",
          "Up Time",
          "Provides the up time of the JVM.",
          Impact.LOW,
          null,
          false,
          BooleanCommandParameter("-date", "Add a prefix with the current date.", true),
        ),
        Command(
          "VM.systemdictionary",
          "System Dictionary",
          "Provides statistics about the dictionary hashtable sizes and bucket length.",
          Impact.MEDIUM,
          null,
          false,
          BooleanCommandParameter(
            "-verbose",
            "Dumps the content of each dictionary entry for all class loaders.",
            mayProduceHighMemoryResult = true,
          ),
        ),
        Command(
          "VM.symboltable",
          "Symbol Table",
          "Dump the symbol table.",
          Impact.MEDIUM,
          null,
          false,
          BooleanCommandParameter(
            "-verbose",
            "Dump the content of each symbol in the table",
            mayProduceHighMemoryResult = true,
          ),
        ),
        Command(
          "VM.stringtable",
          "String Table",
          "Dumps the <code>String</code> table.",
          Impact.MEDIUM,
          null,
          false,
          BooleanCommandParameter(
            "-verbose",
            "Dump the content of each <code>String</code> in the table",
            mayProduceHighMemoryResult = true,
          ),
        ),
        Command(
          "VM.print_touched_methods",
          "Touched Methods",
          "Provides all methods that have ever been touched during the lifetime of this JVM.",
          Impact.MEDIUM,
        ),
        Command(
          "VM.classloaders",
          "Classloaders",
          "Provides a tree view of the classloader hierarchy.",
          Impact.MEDIUM,
          null,
          false,
          BooleanCommandParameter(
            "show-classes",
            "Show loaded classes",
            mayProduceHighMemoryResult = true,
          ),
          BooleanCommandParameter(
            "verbose",
            "Show detailed information",
            mayProduceHighMemoryResult = true,
          ),
          BooleanCommandParameter("fold", "Show loaders of the same name and class as one"),
        ),
        Command(
          "VM.classloader_stats",
          "Classloader Statistics",
          "Provides statistics about all <code>ClassLoader</code>s.",
        ),
        Command("VM.dynlibs", "Dynamic Libraries", "Provides the loaded dynamic libraries."),
        Command(
          "VM.flags",
          "List Flags",
          "Provides the JVM flag options and their current values.",
          Impact.LOW,
          null,
          false,
          BooleanCommandParameter("-all", "Show all flags supported by the JVM"),
        ),
        Command(
          "VM.command_line",
          "Command Line",
          "Provides the used command line to start the JVM.",
        ),
        Command(
          "VM.system_properties",
          "System Properties",
          "Provides the current system properties.",
        ),
        Command(
          "VM.metaspace",
          "Metaspace",
          "Provides the statistics about the metaspace.",
          Impact.MEDIUM,
          Pair(
            "Analyze Metaspace with jcmd VM.metaspace",
            "https://stuefe.de/posts/metaspace/analyze-metaspace-with-jcmd/",
          ),
          false,
          BooleanCommandParameter("basic", "Show a basic summary (does not need a safepoint)"),
          BooleanCommandParameter(
            "show-loaders",
            "Show usage by class loader",
            mayProduceHighMemoryResult = true,
          ),
          BooleanCommandParameter(
            "show-classes",
            "Shows loaded classes for each class loader",
            mayProduceHighMemoryResult = true,
          ),
          BooleanCommandParameter("by-chunktype", "Break down numbers by chunk type"),
          BooleanCommandParameter("by-spacetype", "Break down numbers by loader type"),
          BooleanCommandParameter(
            "vslist",
            "Show details about the underlying virtual space",
            mayProduceHighMemoryResult = true,
          ),
          BooleanCommandParameter(
            "vsmap",
            "Show chunk composition of the underlying virtual spaces",
            mayProduceHighMemoryResult = true,
          ),
          ValuesSelectionCommandParameter(
            "scale",
            "Scale",
            arrayOf("dynamic", "1", "KB", "MB", "GB"),
          ),
        ),
        Command(
          "VM.class_hierarchy",
          "Class Hierarchy",
          "Provides a list of all loaded classes, indented to show the class hierarchy. The name of each class is followed by the ClassLoaderData* of its ClassLoader, or <code>null</code> if it is loaded by the bootstrap class loader.",
          Impact.MEDIUM,
          null,
          false,
          BooleanCommandParameter("-i", "Add inherited interfaces"),
          BooleanCommandParameter("-s", "Only sub classes"),
          OptionalInputCommandParameter(null, "Limit to class"),
        ),
        Command(
          "VM.native_memory",
          "Native Memory",
          "Provides the native memory usage.",
          Impact.MEDIUM,
          null,
          false,
          BooleanCommandParameter("summary", "Show current memory summary"),
          BooleanCommandParameter(
            "detail",
            "Show memory allocation",
            mayProduceHighMemoryResult = true,
          ),
          BooleanCommandParameter(
            "baseline",
            "Baseline current memory usage",
            mayProduceHighMemoryResult = true,
          ),
          BooleanCommandParameter(
            "summary.diff",
            "Summary comparison against previous baseline",
            mayProduceHighMemoryResult = true,
          ),
          BooleanCommandParameter(
            "detail.diff",
            "Detail comparison against previous baseline",
            mayProduceHighMemoryResult = true,
          ),
          BooleanCommandParameter("shutdown", "Shutdown process and free memory"),
          BooleanCommandParameter(
            "statistics",
            "Show tracker statistics",
            mayProduceHighMemoryResult = true,
          ),
          ValuesSelectionCommandParameter(
            "scale",
            "Scale",
            arrayOf("dynamic", "1", "KB", "MB", "GB"),
          ),
        ),
        Command(
          "VM.log",
          "Logger Configuration",
          "Provides the current configuration of the JVM logger.",
          Impact.LOW,
          null,
          mayProduceHighMemoryResult = false,
          FixedCommandParameter("list"),
        ),
        Command(
          "GC.heap_info",
          "Heap Information",
          "Provides information about the current JVM heap.",
          Impact.MEDIUM,
        ),
        Command(
          "GC.class_histogram",
          "Heap Usage",
          "Provides information about the usage of the current JVM heap.",
          Impact.LOW,
          null,
          mayProduceHighMemoryResult = true,
          BooleanCommandParameter("-all", "Inspect all objects, including unreachable objects"),
        ),
        Command(
          "GC.finalizer_info",
          "Finalizer Queue",
          "Provides information about the Java finalization queue.",
          Impact.MEDIUM,
        ),
        Command(
          "GC.class_stats",
          "Loaded Class Meta Data",
          "Provides statistics about the meta data of all loaded classes.",
          Impact.HEIGHT,
          null,
          mayProduceHighMemoryResult = true,
          BooleanCommandParameter("-all", "Show all columns"),
          BooleanCommandParameter("-csv", "Show comma-separated values (CSV)"),
          BooleanCommandParameter("-help", "Show the meaning of all the columns"),
          OptionalInputCommandParameter(null, "Column names"),
        ),
      )
  }

  // -- Properties ---------------------------------------------------------- //

  private lateinit var commandDescriptionLabel: JLabel
  private lateinit var commandSelection: ComboBox<Command>
  private lateinit var commandOptionsWrapper: JPanel

  private val createJToolCommand: (JvmActionContext) -> Pair<JTool, List<String>> = {
    createJToolCommand(it)
  }

  private val taskTitle: (JvmActionContext) -> String = {
    "Getting ${commandSelection.item.title.lowercase()} of PID ${it.processNode.process.processID}"
  }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun createComponent(project: Project, parent: Disposable) =
    JPanel(GridBagLayout()).apply {
      border = UiUtils.EMPTY_BORDER

      val bag = UiUtils.createDefaultGridBag()

      commandSelection = ComboBox(COMMANDS)
      add(commandSelection, bag.nextLine().next())

      commandDescriptionLabel = JLabel(AllIcons.General.ContextHelp)
      add(
        commandDescriptionLabel,
        bag.next().anchor(GridBagConstraints.WEST).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2),
      )

      commandOptionsWrapper = JPanel(GridBagLayout())
      add(
        commandOptionsWrapper,
        bag.nextLine().next().coverLine().weightx(1.0).fillCellHorizontally(),
      )

      val runButton =
        AnActionOptionButton(createOpenResultInPopupRunOption(), createToFileJToolRunOption())
      add(runButton, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP).coverLine())

      commandSelection.addActionListener { syncCommandOptionsWrapper(this) }
      syncCommandOptionsWrapper(this)
    }

  // -- Private Methods ----------------------------------------------------- //

  private fun createOpenResultInPopupRunOption(): AnAction {
    val onSuccess: (String, JvmActionContext) -> Unit = { result, jvmActionContext ->
      TextPopup.showCenteredInCurrentWindow(
        taskTitle(jvmActionContext),
        result,
        jvmActionContext.project,
        false,
      )
    }
    return ToStringJToolRunOption("Run", taskTitle, createJToolCommand, onSuccess) {
      commandSelection.item.mayProduceHighMemoryResult()
    }
  }

  private fun createToFileJToolRunOption(): AnAction {
    return ToFileJToolRunOption(taskTitle, "jvm-information_", createJToolCommand)
  }

  private fun createJToolCommand(jvmActionContext: JvmActionContext): Pair<JTool, List<String>> {
    val processID = jvmActionContext.processNode.process.processID
    return Pair(JTool.JCMD, listOf(processID.toString()) + commandSelection.item.createArguments())
  }

  private fun syncCommandOptionsWrapper(parent: JPanel) {
    commandOptionsWrapper.removeAll()

    val command = commandSelection.item
    commandDescriptionLabel.toolTipText =
      "<html>${command.description}<br><br>${command.impact.displayText} performance impact on the JVM.</html>"

    val bag = UiUtils.createDefaultGridBag()

    command.helpLink?.let {
      commandOptionsWrapper.add(
        createLink(it.first, it.second),
        bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP),
      )
    }

    for (i in command.options.indices) {
      command.options[i].createComponent()?.let {
        UIUtil.setBackgroundRecursively(it, UIUtil.getTreeBackground())
        UIUtil.setForegroundRecursively(it, UIUtil.getTreeForeground())
        commandOptionsWrapper.add(
          it,
          bag
            .nextLine()
            .next()
            .overrideTopInset(UIUtil.DEFAULT_VGAP)
            .weightx(1.0)
            .fillCellHorizontally(),
        )
      }
    }

    parent.revalidate()
    parent.repaint()
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class Command(
    val command: String,
    val title: String,
    val description: String,
    val impact: Impact = Impact.LOW,
    val helpLink: Pair<String, String>? = null,
    val mayProduceHighMemoryResult: Boolean = false,
    vararg val options: JcmdCommandParameter,
  ) {

    override fun toString() = title

    fun mayProduceHighMemoryResult(): Boolean =
      mayProduceHighMemoryResult || options.any { it.mayProduceHighMemoryResult() }

    fun createArguments() = listOf(command) + options.flatMap { it.createCommandArguments() }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private abstract class JcmdCommandParameter {

    /**
     * It's important that the implementations are not reusing components. Because during a theme
     * change, they may not get any color changed events if they are not part of the current
     * components tree.
     */
    abstract fun createComponent(): JComponent?

    abstract fun createCommandArguments(): List<String>

    open fun mayProduceHighMemoryResult(): Boolean = false
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class BooleanCommandParameter(
    val argument: String,
    val description: String,
    val defaultSelected: Boolean = false,
    val mayProduceHighMemoryResult: Boolean = false,
  ) : JcmdCommandParameter() {

    private lateinit var checkbox: JBCheckBox

    override fun createComponent(): JComponent {
      checkbox = JBCheckBox("<html>${description}</html>", defaultSelected)
      return checkbox
    }

    override fun createCommandArguments(): List<String> {
      return if (checkbox.isSelected) listOf(argument) else emptyList()
    }

    override fun mayProduceHighMemoryResult(): Boolean {
      return checkbox.isSelected && mayProduceHighMemoryResult
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class ValuesSelectionCommandParameter(
    val argument: String,
    val description: String,
    val values: Array<String>,
  ) : JcmdCommandParameter() {

    private lateinit var comboBox: ComboBox<String>

    @Suppress("UnstableApiUsage")
    override fun createComponent() =
      JPanel(ListLayout.horizontal(UIUtil.DEFAULT_HGAP / 2)).apply {
        add(JBLabel("$description:"))

        comboBox = ComboBox(values)
        add(comboBox)
      }

    override fun createCommandArguments() = listOf("$argument=${comboBox.item}")
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class OptionalInputCommandParameter(
    val argument: String?,
    val description: String,
    val defaultSelected: Boolean = false,
    val mayProduceHighMemoryResult: Boolean = false,
  ) : JcmdCommandParameter() {

    private lateinit var checkbox: JBCheckBox
    private lateinit var inputField: JBTextField

    override fun createComponent(): JComponent =
      BorderLayoutPanel(UIUtil.DEFAULT_HGAP / 2, UIUtil.DEFAULT_VGAP).apply {
        checkbox =
          JBCheckBox("<html>${description}:</html>", defaultSelected).apply {
            addChangeListener { syncEnabledState() }
          }
        addToLeft(checkbox)

        inputField = JBTextField().apply { isEnabled = defaultSelected }
        addToCenter(inputField)
      }

    override fun createCommandArguments(): List<String> {
      if (checkbox.isSelected) {
        return listOf((if (argument != null) "$argument=${inputField.text ?: ""}" else ""))
      } else {
        return emptyList()
      }
    }

    override fun mayProduceHighMemoryResult(): Boolean = mayProduceHighMemoryResult

    private fun syncEnabledState() {
      inputField.isEnabled = checkbox.isEnabled
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class FixedCommandParameter(val argument: String) : JcmdCommandParameter() {

    override fun createComponent(): JComponent? = null

    override fun createCommandArguments(): List<String> = listOf(argument)
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class Impact(val displayText: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HEIGHT("Height"),
  }
}
