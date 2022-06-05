package dev.turingcomplete.intellijjpsplugin.process.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.list.JavaProcessesTable
import javax.swing.Icon

class GracefullyTerminateProcessesAction
  : TerminateProcessesAction<GracefullyTerminateProcessesAction>(JavaProcessesTable.SELECTED_PROCESSES) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(GracefullyTerminateProcessesAction::class.java)
  }

  // -- Variables --------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun icon(): Icon = AllIcons.Actions.Suspend

  override fun createTitle(dataContext: DataContext): String {
    val numOfDaemons = getProcessNodes(dataContext).size
    return if (numOfDaemons == 1) "Gracefully Terminate Daemon" else "Gracefully Terminate $numOfDaemons Processes"
  }

  override fun createErrorMessage(processNode: ProcessNode, error: Throwable): String {
    return "Failed to gracefully terminate process with PID ${processNode.process.processID}: ${error.message}"
  }

  override fun terminate(processNode: ProcessNode, progressIndicator: ProgressIndicator) {
    val message = "Gracefully terminating process with PID ${processNode.process.processID}."
    progressIndicator.text2 = message
    LOG.info(message)

    ProcessHandle.of(processNode.process.processID.toLong()).ifPresent {
      it.destroy()
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}