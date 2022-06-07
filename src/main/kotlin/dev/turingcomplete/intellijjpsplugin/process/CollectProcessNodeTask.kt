package dev.turingcomplete.intellijjpsplugin.process

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

class CollectProcessNodeTask(private val pid: Int,
                             project: Project?,
                             private val onSuccess: (ProcessNode?) -> Unit,
                             private val onFinished: () -> Unit,
                             private val onThrowable: (Throwable) -> Unit)
  : Task.ConditionalModal(project, "Collecting process information", true, ALWAYS_BACKGROUND) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var processNode: ProcessNode? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun run(processIndicator: ProgressIndicator) {
    processNode = OshiUtils.OPERATION_SYSTEM.getProcess(pid)?.let { ProcessNode(it) }
  }

  override fun onSuccess() {
    onSuccess(processNode)
  }

  override fun onFinished() {
    onFinished.invoke()
  }

  override fun onThrowable(error: Throwable) {
    onThrowable.invoke(error)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}