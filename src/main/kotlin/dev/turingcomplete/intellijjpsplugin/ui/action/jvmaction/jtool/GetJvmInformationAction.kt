package dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.jtool

import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.newui.HorizontalLayout
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.JvmAction
import dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.JvmActionContext
import dev.turingcomplete.intellijjpsplugin.ui.common.*
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils.createLink
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class GetJvmInformationAction : JvmAction("Get JVM Information") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  /*
  VM.class_hierarchy
VM.log
VM.native_memory
   */

  companion object {
    private val COMMANDS =
      arrayOf(Command("VM.version", "Version", "Prints JVM version information."),
              Command("VM.info", "Information", "Prints information about the JVM environment and status."),
              Command("VM.uptime", "Up Time", "Prints the VM up time.", Impact.LOW, null,
                      BooleanCommandOption("-date", "Add a prefix with the current date.", true)),
              Command("VM.systemdictionary", "System Dictionary", "Prints the statistics for dictionary hashtable sizes and bucket length.", Impact.MEDIUM, null,
                      BooleanCommandOption("-verbose", "Dumps the content of each dictionary entry for all class loaders.", mayProduceHighMemoryResult = true)),
              Command("VM.symboltable", "Symbol Table", "Dump the symbol table.", Impact.MEDIUM, null,
                      BooleanCommandOption("-verbose", "Dump the content of each symbol in the table", mayProduceHighMemoryResult = true)),
              Command("VM.stringtable", "String Table", "Dumps the <code>String</code> table.", Impact.MEDIUM, null,
                      BooleanCommandOption("-verbose", "Dump the content of each <code>String</code> in the table", mayProduceHighMemoryResult = true)),
              Command("VM.print_touched_methods", "Touched Methods", "Prints all methods that have ever been touched during the lifetime of this JVM.", Impact.MEDIUM),
              Command("VM.classloaders", "Classloaders", "Prints classloader hierarchy.", Impact.MEDIUM, null,
                      BooleanCommandOption("show-classes", "Print loaded classes", mayProduceHighMemoryResult = true),
                      BooleanCommandOption("verbose", "Print detailed information", mayProduceHighMemoryResult = true),
                      BooleanCommandOption("fold", "Show loaders of the same name and class as one")),
              Command("VM.classloader_stats", "Classloader Statistics", "Prints statistics about all <code>ClassLoader</code>s."),
              Command("VM.dynlibs", "Dynamic Libraries", "Prints the loaded dynamic libraries."),
              Command("VM.flags", "List Flags", "Prints the VM flag options and their current values.", Impact.LOW, null,
                      BooleanCommandOption("-all", "Print all flags supported by the VM")),
              Command("VM.command_line", "Command Line", "Prints the command line used to start this VM instance."),
              Command("VM.system_properties", "System Properties", "Prints the system properties."),
              Command("VM.metaspace", "Metaspace", "Prints the statistics for the Metaspace.", Impact.MEDIUM,
                      Pair("Analyze Metaspace with jcmd VM.metaspace", "https://stuefe.de/posts/metaspace/analyze-metaspace-with-jcmd/"),
                      BooleanCommandOption("basic", "Print a basic summary (does not need a safepoint)"),
                      BooleanCommandOption("show-loaders", "Show usage by class loader", mayProduceHighMemoryResult = true),
                      BooleanCommandOption("show-classes", "Shows loaded classes for each class loader", mayProduceHighMemoryResult = true),
                      BooleanCommandOption("by-chunktype", "Break down numbers by chunk type"),
                      BooleanCommandOption("by-spacetype", "Break down numbers by loader type"),
                      BooleanCommandOption("vslist", "Show details about the underlying virtual space", mayProduceHighMemoryResult = true),
                      BooleanCommandOption("vsmap", "Show chunk composition of the underlying virtual spaces", mayProduceHighMemoryResult = true),
                      ValuesSelectionCommandOption("scale", "Scale", arrayOf("dynamic", "1", "KB", "MB", "GB"))))
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val commandDescriptionLabel = JLabel(AllIcons.General.ContextHelp)
  private val commandSelection = ComboBox(COMMANDS)
  private val commandOptionsWrapper = JPanel(GridBagLayout())

  private val createJToolCommand: (JvmActionContext) -> Pair<JTool, List<String>> = { createJToolCommand(it) }

  private val taskTitle : (JvmActionContext) -> String = {
    "${commandSelection.item.title} of PID ${it.processNode.process.processID}"
  }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent() = JPanel(GridBagLayout()).apply {
    border = UiUtils.EMPTY_BORDER

    val bag = UiUtils.createDefaultGridBag()

    add(commandSelection, bag.nextLine().next())
    add(commandDescriptionLabel, bag.next().anchor(GridBagConstraints.WEST).overrideLeftInset(UIUtil.DEFAULT_HGAP))

    add(commandOptionsWrapper, bag.nextLine().next().coverLine().weightx(1.0).fillCellHorizontally())

    val runButton = AnActionOptionButton(createOpenResultInPopupRunOption(), createToFileJToolRunOption())
    add(runButton, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP).coverLine())

    commandSelection.addActionListener { syncCommandOptionsWrapper(this) }
    syncCommandOptionsWrapper(this)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createOpenResultInPopupRunOption(): AnAction {
    val onSuccess: (String, JvmActionContext) -> Unit = { result, jvmActionContext ->
      TextPopup.showCenteredInCurrentWindow(taskTitle(jvmActionContext), result, jvmActionContext.project, false)
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
    commandDescriptionLabel.toolTipText = "<html>${command.description}<br><br>${command.impact.displayText} performance impact on the target JVM.</html>"

    val bag = UiUtils.createDefaultGridBag()

    command.helpLink?.let {
      commandOptionsWrapper.add(createLink(it.first, it.second), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    }

    for (i in command.options.indices) {
      val option = command.options[i]
      commandOptionsWrapper.add(option.createComponent(), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP).weightx(1.0).fillCellHorizontally())
    }

    parent.revalidate()
    parent.repaint()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class Command(val command: String,
                        val title: String,
                        val description: String,
                        val impact: Impact = Impact.LOW,
                        val helpLink: Pair<String, String>? = null,
                        vararg val options: JcmdCommandOption) {

    override fun toString() = title

    fun mayProduceHighMemoryResult(): Boolean = options.any { it.mayProduceHighMemoryResult() }

    fun createArguments() = listOf(command) + options.flatMap { it.createCommandArguments() }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private abstract class JcmdCommandOption() {

    abstract fun createComponent(): JComponent

    abstract fun createCommandArguments(): List<String>

    open fun mayProduceHighMemoryResult(): Boolean = false
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class BooleanCommandOption(val argument: String,
                                     description: String,
                                     defaultSelected: Boolean = false,
                                     val mayProduceHighMemoryResult: Boolean = false) : JcmdCommandOption() {

    val checkbox = JBCheckBox("<html>${description}</html>", defaultSelected)

    override fun createComponent(): JComponent = checkbox

    override fun createCommandArguments(): List<String> {
      return if (checkbox.isSelected) listOf(argument) else emptyList()
    }

    override fun mayProduceHighMemoryResult(): Boolean {
      return checkbox.isSelected && mayProduceHighMemoryResult
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ValuesSelectionCommandOption(val argument: String,
                                             val description: String,
                                             values: Array<String>) : JcmdCommandOption() {

    val comboBox = ComboBox(values)

    override fun createComponent() = JPanel(HorizontalLayout(UIUtil.DEFAULT_HGAP / 2)).apply {
      add(JBLabel("$description:"))
      add(comboBox)
    }

    override fun createCommandArguments() = listOf("$argument=${comboBox.item}")
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class Impact(val displayText: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HEIGHT("Height")
  }
}