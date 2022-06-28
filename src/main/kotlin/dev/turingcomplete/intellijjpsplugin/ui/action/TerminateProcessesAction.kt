package dev.turingcomplete.intellijjpsplugin.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.util.concurrency.AppExecutorUtil
import dev.turingcomplete.intellijjpsplugin.JpsPluginService
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.CommonsDataKeys
import dev.turingcomplete.intellijjpsplugin.ui.common.NotificationUtils
import java.util.concurrent.TimeUnit
import javax.swing.Icon

abstract class TerminateProcessesAction<T : TerminateProcessesAction<T>>(private val collectJavaProcessesOnSuccess: Boolean,
                                                                         private val processNodesDataKey: DataKey<List<ProcessNode>>)
  : DumbAwareAction("Terminate Processes", "Terminate processes", AllIcons.Debugger.KillProcess), DumbAware {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(TerminateProcessesAction::class.java)
  }

  // -- Variables --------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  final override fun update(e: AnActionEvent) {
    super.update(e)

    e.presentation.icon = icon()
    e.presentation.text = createTitle(e.dataContext)
  }

  final override fun actionPerformed(e: AnActionEvent) {
    TerminateProcessesTask(e.project, createTitle(e.dataContext),
                           getProcessNodes(e.dataContext),
                           collectJavaProcessesOnSuccess,
                           { processNode, progressIndicator -> terminate(processNode, progressIndicator) },
                           { processNode, error -> createErrorMessage(processNode, error) })
            .queue()
  }

  abstract fun createTitle(dataContext: DataContext): String

  abstract fun terminate(processNode: ProcessNode, progressIndicator: ProgressIndicator)

  abstract fun createErrorMessage(processNode: ProcessNode, error: Throwable): String

  abstract fun icon(): Icon

  protected fun getProcessNodes(dataContext: DataContext): List<ProcessNode> {
    return CommonsDataKeys.getRequiredData(processNodesDataKey, dataContext)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class TerminateProcessesTask(project: Project?,
                                       title: String,
                                       val processNodes: List<ProcessNode>,
                                       val collectJavaProcessesOnSuccess: Boolean,
                                       val terminateProcess: (ProcessNode, ProgressIndicator) -> Unit,
                                       val createErrorMessage: (ProcessNode, Throwable) -> String)

    : Task.ConditionalModal(project, title, true, ALWAYS_BACKGROUND) {

    private val deferredFailures = mutableListOf<String>()

    override fun run(progressIndicator: ProgressIndicator) {
      processNodes.forEach { processNode ->
        try {
          terminateProcess(processNode, progressIndicator)
        }
        catch (e: Exception) {
          deferredFailures.add(createErrorMessage(processNode, e))
        }
      }
    }

    override fun onSuccess() {
      if (collectJavaProcessesOnSuccess) {
        // Wait a second to give the process time to terminate
        AppExecutorUtil.getAppScheduledExecutorService()
                .schedule({ project.getService(JpsPluginService::class.java).collectJavaProcesses() },
                          1, TimeUnit.SECONDS)
      }

      val message = if (processNodes.size > 1) {
        "Termination of the processes with PIDs ${processNodes.joinToString { it.process.processID.toString() }} was triggered."
      }
      else {
        "Termination of process with PID ${processNodes[0].process.processID} was triggered."
      }
      NotificationUtils.notifyOnToolWindow(message, project)

      project.getService(JpsPluginService::class.java).processDetailsUpdated(processNodes)
    }

    override fun onThrowable(error: Throwable) {
      LOG.warn("Failed to execute process termination task.", error)
      ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(project,
                                 "Failed to execute process termination task: ${error.message}\nSee idea.log for more details.",
                                 "Terminate Processes Failed")
      }
    }

    override fun onFinished() {
      if (deferredFailures.isNotEmpty()) {
        ApplicationManager.getApplication().invokeLater {
          Messages.showErrorDialog(project, deferredFailures.joinToString("\n\n"), "Terminate Processes Failed")
        }
      }
    }
  }
}
