package dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.jtool

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.ui.components.JBCheckBox
import com.intellij.unscramble.UnscrambleDialog
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.JvmAction
import dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.JvmActionContext
import dev.turingcomplete.intellijjpsplugin.ui.common.AnActionOptionButton
import dev.turingcomplete.intellijjpsplugin.ui.common.TextPopup
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjpsplugin.ui.common.overrideTopInset
import java.awt.GridBagLayout
import javax.swing.JPanel

class CreateJvmThreadDumpAction : JvmAction("Creating Thread Dump") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val TASK_TITLE : (JvmActionContext) -> String = {
      "Creating Thread Dump of PID ${it.processNode.process.processID}"
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val printAdditionalLocksInformation = JBCheckBox("Print additional information about locks", true)
  private val printAdditionalThreadsInformation = JBCheckBox("Print additional information about threads", true)

  private val createJToolCommand: (JvmActionContext) -> Pair<JTool, List<String>> = { createJToolCommand(it) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent() = JPanel(GridBagLayout()).apply {
    border = UiUtils.EMPTY_BORDER

    val bag = UiUtils.createDefaultGridBag()

    add(printAdditionalLocksInformation, bag.nextLine().next().weightx(1.0).fillCellHorizontally())
    add(printAdditionalThreadsInformation, bag.nextLine().next().weightx(1.0).fillCellHorizontally())

    val runButton = AnActionOptionButton(createOpenResultInThreadDumpAnalyzerRunOption(),
                                         createOpenResultInPopupRunOption(),
                                         createToFileJToolRunOption())
    add(runButton, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP))
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createOpenResultInThreadDumpAnalyzerRunOption(): AnAction {
    val onSuccess: (String, JvmActionContext) -> Unit = { result, jvmActionContext ->
      val unscrambleDialog = UnscrambleDialog(jvmActionContext.project)
      unscrambleDialog.setText(result)
      unscrambleDialog.show()
    }
    return ToStringJToolRunOption("Run - Open In Thread Dump Analyzer", TASK_TITLE, createJToolCommand, onSuccess)
  }

  private fun createOpenResultInPopupRunOption(): AnAction {
    val onSuccess: (String, JvmActionContext) -> Unit = { result, jvmActionContext ->
      val title = "Thread Dump of PID ${jvmActionContext.processNode.process.processID}"
      TextPopup.showCenteredInCurrentWindow(title, result, jvmActionContext.project, false)
    }
    return ToStringJToolRunOption("Run", TASK_TITLE, createJToolCommand, onSuccess)
  }

  private fun createToFileJToolRunOption(): AnAction {
    return ToFileJToolRunOption(TASK_TITLE, "thread-dump_", createJToolCommand)
  }

  private fun createJToolCommand(jvmActionContext: JvmActionContext): Pair<JTool, List<String>> {
    val command = mutableListOf<String>()

    if (printAdditionalLocksInformation.isSelected) {
      command.add("-l")
    }
    if (printAdditionalThreadsInformation.isSelected) {
      command.add("-e")
    }
    command.add(jvmActionContext.processNode.process.processID.toString())

    return Pair(JTool.JSTACK, command)
  }
}