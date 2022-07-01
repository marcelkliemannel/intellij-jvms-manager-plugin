package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.jtool

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.AnActionOptionButton
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.overrideLeftInset
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmAction
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmActionContext
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel

class JvmMonitoringAction : JvmAction("Monitoring") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent() = JPanel(GridBagLayout()).apply {
    border = UiUtils.EMPTY_BORDER

    val bag = UiUtils.createDefaultGridBag()

    add(AnActionOptionButton(createStartJConsoleRunOption()), bag.nextLine().next())
    add(UiUtils.createContextHelpLabel("<html>Starts a graphical interface to monitor and manage Java virtual " +
                                       "machines.<br><br>May have a medium performance impact on the JVM.</html>"),
        bag.next().anchor(GridBagConstraints.WEST).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2))

    // Stretch panel horizontally
    add(UiUtils.EMPTY_FILL_PANEL(), bag.nextLine().next().coverLine().weightx(1.0).fillCellHorizontally())
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createStartJConsoleRunOption(): AnAction {
    val commandLine: (JvmActionContext) -> Pair<JTool, List<String>> = {
      Pair(JTool.JCONSOLE, listOf(it.processNode.process.processID.toString()))
    }
    return NotWaitingJToolRunOption("Start JConsole",
                                    { "Starting JConsole for PID ${it.processNode.process.processID}" },
                                    commandLine)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}