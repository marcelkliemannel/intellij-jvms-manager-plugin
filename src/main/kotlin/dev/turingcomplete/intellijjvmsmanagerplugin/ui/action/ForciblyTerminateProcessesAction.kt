package dev.turingcomplete.intellijjvmsmanagerplugin.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode
import javax.swing.Icon

class ForciblyTerminateProcessesAction(collectJavaProcessesOnSuccess: Boolean)
  : TerminateProcessesAction<ForciblyTerminateProcessesAction>(collectJavaProcessesOnSuccess) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(GracefullyTerminateProcessesAction::class.java)
  }

  // -- Variables --------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun icon(): Icon = AllIcons.Debugger.KillProcess

  override fun createErrorMessage(processNode: ProcessNode, error: Throwable): String {
    return "Failed to forcibly terminating process with PID ${processNode.process.processID}: ${error.message}"
  }

  override fun createTitle(dataContext: DataContext): String {
    val numOfProcesses = getProcessNodes(dataContext).size
    return if (numOfProcesses == 1) "Forcibly Terminate Process" else "Forcibly Terminate $numOfProcesses Processes"
  }

  override fun terminate(processNode: ProcessNode, progressIndicator: ProgressIndicator) {
    val message = "Forcibly terminating process with PID ${processNode.process.processID}..."
    progressIndicator.text2 = message
    LOG.info(message)

    processNode.terminateForcibly()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}