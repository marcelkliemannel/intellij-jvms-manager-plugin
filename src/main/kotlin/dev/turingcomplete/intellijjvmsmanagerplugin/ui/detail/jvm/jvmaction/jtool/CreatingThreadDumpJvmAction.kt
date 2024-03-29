package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.jtool

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.unscramble.UnscrambleDialog
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.AnActionOptionButton
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.TextPopup
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.overrideTopInset
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmAction
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmActionContext
import java.awt.GridBagLayout
import javax.swing.JPanel

class CreatingThreadDumpJvmAction : JvmAction("Thread Dump") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val TASK_TITLE : (JvmActionContext) -> String = {
      "Creating Thread Dump of PID ${it.processNode.process.processID}"
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var printAdditionalLocksInformation: JBCheckBox
  private lateinit var  printAdditionalThreadsInformation: JBCheckBox

  private val createJToolCommand: (JvmActionContext) -> Pair<JTool, List<String>> = { createJToolCommand(it) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent(project: Project, parent: Disposable) = JPanel(GridBagLayout()).apply {
    border = UiUtils.EMPTY_BORDER

    val bag = UiUtils.createDefaultGridBag()

    printAdditionalLocksInformation = JBCheckBox("Print additional information about locks", true)
    printAdditionalThreadsInformation = JBCheckBox("Print additional information about threads", true)
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
    return ToStringJToolRunOption("Run - Show In Thread Dump Analyzer", TASK_TITLE, createJToolCommand, onSuccess)
  }

  private fun createOpenResultInPopupRunOption(): AnAction {
    val onSuccess: (String, JvmActionContext) -> Unit = { result, jvmActionContext ->
      val title = "Thread Dump of PID ${jvmActionContext.processNode.process.processID}"
      TextPopup.showCenteredInCurrentWindow(title, result, jvmActionContext.project, false)
    }
    return ToStringJToolRunOption("Run - Show Output", TASK_TITLE, createJToolCommand, onSuccess)
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