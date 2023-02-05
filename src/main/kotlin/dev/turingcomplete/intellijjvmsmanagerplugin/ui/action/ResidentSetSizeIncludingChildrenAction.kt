package dev.turingcomplete.intellijjvmsmanagerplugin.ui.action

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.text.StringUtil
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.CommonsDataKeys

class ResidentSetSizeIncludingChildrenAction: DumbAwareAction() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    val processes = CommonsDataKeys.getRequiredData(CommonsDataKeys.SELECTED_PROCESSES_DATA_KEY, e.dataContext)
    if (processes.size == 1 && processes[0].childCount > 0) {
      e.presentation.isEnabled = false
      e.presentation.isVisible = true
      e.presentation.text = "Resident Set Size (RSS) including children: ${StringUtil.formatFileSize(sumUpResidentSetSize(processes[0]))}"
    }
    else {
      e.presentation.isEnabledAndVisible = false
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    throw IllegalStateException("snh: This action is not executable")
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun sumUpResidentSetSize(processNode: ProcessNode): Long {
    return processNode.process.residentSetSize + processNode.children().asSequence().sumOf {
      sumUpResidentSetSize(it as ProcessNode)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}