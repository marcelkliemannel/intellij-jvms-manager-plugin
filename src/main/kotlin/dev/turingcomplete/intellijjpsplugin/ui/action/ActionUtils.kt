package dev.turingcomplete.intellijjpsplugin.ui.action

import com.intellij.openapi.actionSystem.DataKey
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode

object ActionUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val SELECTED_PROCESSES: DataKey<List<ProcessNode>> = DataKey.create("JavaProcessesPlugin.SelectedProcesses")
  val SELECTED_PROCESS: DataKey<ProcessNode> = DataKey.create("JavaProcessesPlugin.SelectedProcess")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}