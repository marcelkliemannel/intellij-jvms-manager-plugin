package dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.jvmaction.jtool

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjpsplugin.ui.common.AnActionOptionButton
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjpsplugin.ui.common.overrideLeftInset
import dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.jvmaction.JvmAction
import dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.jvmaction.JvmActionContext
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
    add(UiUtils.createContextHelpLabel("Starts a graphical interface to monitor and manage Java virtual machines."), bag.next().anchor(GridBagConstraints.WEST).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2))

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