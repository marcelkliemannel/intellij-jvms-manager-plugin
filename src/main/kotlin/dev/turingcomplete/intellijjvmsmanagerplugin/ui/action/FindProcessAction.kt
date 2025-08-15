package dev.turingcomplete.intellijjvmsmanagerplugin.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import dev.turingcomplete.intellijjvmsmanagerplugin.JvmsManagerPluginService
import dev.turingcomplete.intellijjvmsmanagerplugin.process.FindProcessNodeTask
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.IntInputValidator

class FindProcessAction : DumbAwareAction("Find Process Information", null, AllIcons.Actions.Find) {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(FindProcessAction::class.java)
  }

  // -- Properties ---------------------------------------------------------- //

  private var findProcessNodeTaskRunning = false

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = !findProcessNodeTaskRunning
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun actionPerformed(e: AnActionEvent) {
    if (findProcessNodeTaskRunning) {
      return
    }

    val pid =
      Messages.showInputDialog(
          e.project,
          "Please enter a PID:",
          "Find Process Information",
          null,
          null,
          IntInputValidator.INSTANCE,
        )
        ?.toIntOrNull() ?: return

    val onSuccess: (ProcessNode?) -> Unit = {
      if (it != null) {
        e.project?.getService(JvmsManagerPluginService::class.java)?.showProcessDetails(it)
      } else {
        Messages.showErrorDialog(
          e.project,
          "No process with PID $pid found.",
          "Finding Process Information Failed",
        )
      }
    }

    val onThrowable: (Throwable) -> Unit = { error ->
      val errorMessage = "Failed to find process: ${error.message}"
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(
        e.project,
        "$errorMessage\n\nSee idea.log for more details.",
        "Finding Process Information Failed",
      )
    }

    findProcessNodeTaskRunning = true
    FindProcessNodeTask(
        pid,
        e.project,
        onSuccess,
        { findProcessNodeTaskRunning = false },
        onThrowable,
      )
      .queue()
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
