package dev.turingcomplete.intellijjpsplugin.jps.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import dev.turingcomplete.intellijjpsplugin.jps.ProcessNode
import javax.swing.Icon

abstract class TerminateProcessesAction<T : TerminateProcessesAction<T>>(private val processNodesDataKey: DataKey<List<ProcessNode>>)
  : AnAction("Terminate Processes", "Terminate processes", AllIcons.Debugger.KillProcess), DumbAware {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(TerminateProcessesAction::class.java)
  }

  // -- Variables --------------------------------------------------------------------------------------------------- //

  private var onFinished: () -> Unit = {}

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  final override fun update(e: AnActionEvent) {
    super.update(e)

    e.presentation.icon = icon()
    e.presentation.text = createTitle(e.dataContext)
  }

  final override fun actionPerformed(e: AnActionEvent) {
    TerminateProcessesTask(e.project, createTitle(e.dataContext),
                           gradleProcessNodes(e.dataContext),
                           { processNode, progressIndicator -> terminate(processNode, progressIndicator) },
                           { processNode, error -> createErrorMessage(processNode, error) },
                           onFinished)
            .queue()
  }

  abstract fun createTitle(dataContext: DataContext): String

  abstract fun terminate(processNode: ProcessNode, progressIndicator: ProgressIndicator)

  abstract fun createErrorMessage(processNode: ProcessNode, error: Throwable): String

  abstract fun icon(): Icon

  protected fun gradleProcessNodes(dataContext: DataContext): List<ProcessNode> {
    return processNodesDataKey.getData(dataContext)
           ?: throw IllegalStateException("Data context is missing required data key '${processNodesDataKey.name}'.")
  }

  fun onFinished(onFinished: () -> Unit): T {
    this.onFinished = onFinished
    @Suppress("UNCHECKED_CAST")
    return this as T
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class TerminateProcessesTask(project: Project?,
                                       title: String,
                                       val processNodes: List<ProcessNode>,
                                       val terminate: (ProcessNode, ProgressIndicator) -> Unit,
                                       val createErrorMessage: (ProcessNode, Throwable) -> String,
                                       val onFinished: () -> Unit)

    : Task.ConditionalModal(project, title, true, ALWAYS_BACKGROUND) {

    private val deferredFailures = mutableListOf<String>()

    override fun run(progressIndicator: ProgressIndicator) {
      processNodes.forEach { processNode ->
        try {
          terminate(processNode, progressIndicator)
        }
        catch (e: Exception) {
          deferredFailures.add(createErrorMessage(processNode, e))
        }
      }
    }

    override fun onThrowable(error: Throwable) {
      LOG.warn("Failed to execute process termination task.", error)
      ApplicationManager.getApplication().invokeLater {
        Messages.showErrorDialog(project,
                                 "Failed to execute process termination task: ${error.message}\nSee idea.log " +
                                 "for more details.\nIf you think this error should not appear, please report a bug.",
                                 "Terminate Processes Failed")
      }
    }

    override fun onFinished() {
      if (deferredFailures.isNotEmpty()) {
        ApplicationManager.getApplication().invokeLater {
          Messages.showErrorDialog(project, deferredFailures.joinToString("\n\n"), "Terminate Processes Failed")
        }
      }
      onFinished.invoke()
    }
  }
}
