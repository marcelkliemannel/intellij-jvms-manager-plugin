package dev.turingcomplete.intellijjvmsmanagerplugin.ui.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import dev.turingcomplete.intellijjvmsmanagerplugin.JvmsManagerPluginService
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.CommonsDataKeys
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.JvmProcessesMainPanel
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.NotificationUtils

class RefreshProcessInformationAction(showIcon: Boolean = true)
  : DumbAwareAction("Refresh Process Information", null, if (showIcon) AllIcons.Actions.Refresh else null) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var updateProcessInformationTaskRunning = false

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = !updateProcessInformationTaskRunning
  }

  override fun actionPerformed(e: AnActionEvent) {
    val processNode = CommonsDataKeys.getRequiredData(CommonsDataKeys.CURRENT_PROCESS_DETAILS_DATA_KEY, e.dataContext)

    updateProcessInformationTaskRunning = true
    UpdateProcessInformationTask(processNode, e.project) { updateProcessInformationTaskRunning = false }.queue()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class UpdateProcessInformationTask(val processNode: ProcessNode,
                                             project: Project?,
                                             val onFinished: () -> Unit)
    : Task.ConditionalModal(project, "Updating process information", false, DEAF) {

    companion object {
      private val LOG = Logger.getInstance(JvmProcessesMainPanel::class.java)
    }

    override fun run(indicator: ProgressIndicator) {
      processNode.update()
    }

    override fun onThrowable(error: Throwable) {
      val errorMessage = "Failed to update information of PID ${processNode.process.processID}."
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(project, "$errorMessage\n\nSee idea.log for more details.", "Updating Process Information Failed")
    }

    override fun onSuccess() {
      project?.getService(JvmsManagerPluginService::class.java)?.processDetailsUpdated(listOf(processNode))

      NotificationUtils.notifyOnToolWindow("Information of process with PID $processNode updated.", project)
    }

    override fun onFinished() {
      onFinished.invoke()
    }
  }
}