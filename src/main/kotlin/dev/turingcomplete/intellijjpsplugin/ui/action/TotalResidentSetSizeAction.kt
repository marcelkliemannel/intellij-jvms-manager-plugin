package dev.turingcomplete.intellijjpsplugin.ui.action

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.text.StringUtil
import dev.turingcomplete.intellijjpsplugin.ui.CommonsDataKeys

class TotalResidentSetSizeAction: DumbAwareAction() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    val processes = CommonsDataKeys.getRequiredData(CommonsDataKeys.SELECTED_PROCESSES_DATA_KEY, e.dataContext)
    if (processes.size > 1) {
      e.presentation.isEnabled = false
      e.presentation.isVisible = true
      e.presentation.text = "Total Resident Set Size (RSS): ${StringUtil.formatFileSize(processes.sumOf { it.process.residentSetSize })}"
    }
    else {
      e.presentation.isEnabledAndVisible = false
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    throw IllegalStateException("snh: This action is not executable")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}