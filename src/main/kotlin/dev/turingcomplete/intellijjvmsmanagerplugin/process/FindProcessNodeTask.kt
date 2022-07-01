package dev.turingcomplete.intellijjvmsmanagerplugin.process

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.sun.tools.attach.VirtualMachine

class FindProcessNodeTask(private val pid: Int,
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
    val process = OshiUtils.OPERATION_SYSTEM.getProcess(pid) ?: return
    val vmDescriptor = VirtualMachine.list().find { pid == it.id().toIntOrNull() }
    processNode = if (vmDescriptor != null) JvmProcessNode(process, vmDescriptor) else ProcessNode(process)
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