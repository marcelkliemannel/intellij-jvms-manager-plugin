package dev.turingcomplete.intellijjpsplugin.process

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.first
import com.sun.tools.attach.VirtualMachine
import oshi.software.os.OSProcess
import oshi.software.os.OperatingSystem
import oshi.software.os.OperatingSystem.ProcessSorting

/**
 * Collects all known [JvmProcessNode], which can be retrieved via the
 * [VirtualMachine] API.
 *
 * If a Java process is a child process of another Java process, it will be
 * added as a child node to its parent process node, and will not be present
 * in the top-level list.
 *
 * The current implementation retrieves a list of all [OSProcess] at once,
 * searches for the [OSProcess] of all known JVM PIDs in that list and then
 * recursively builds the children tree based on that list. An alternative
 * implementation could use the methods [OperatingSystem.getProcess] and
 * [OperatingSystem.getChildProcesses] from the Oshi library. However, after
 * some performance testing, calling these methods multiple times is several
 * orders of magnitude slower than the current approach. (The reason for this,
 * at least until Oshi version 6, is that each of these methods first retrieves
 * all processes and then filters out only the desired ones.)
 */
class CollectJavaProcessNodesTask(project: Project?,
                                  private val onSuccess: (List<JvmProcessNode>) -> Unit,
                                  private val onFinished: () -> Unit,
                                  private val onThrowable: (Throwable) -> Unit)
  : Task.ConditionalModal(project, "Collecting Java processes information", true, ALWAYS_BACKGROUND) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(CollectJavaProcessNodesTask::class.java)
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val topLevelJvmProcessNodes = mutableListOf<JvmProcessNode>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun run(progressIndicator: ProgressIndicator) {
    val startMillis = System.currentTimeMillis()

    topLevelJvmProcessNodes.clear()

    // Collect all processes current running on the machine.
    val allProcesses = OshiUtils.OPERATION_SYSTEM.getProcesses(null, ProcessSorting.NO_SORTING, 0)
            .associateBy { it.processID }

    // Collect all known Java processes.
    // All real top-level Java processes will be added to the result
    // `topLevelJavaProcessNodes`, but they won't have any children yet. All
    // other java processes will be added to the `remainingJavaProcessNodesToAssociate`.
    // These are either children of a real top-level Java process or are
    // children of a non-Java process. In the latter case, they will be later
    // handled as if they are top-level Java processes.
    progressIndicator.checkCanceled()
    val remainingJvmProcessNodesToAssociate = mutableMapOf<Int, JvmProcessNode>()
    VirtualMachine.list().forEach { vmDescriptor ->
      val pid = vmDescriptor.id().toIntOrNull() ?: return@forEach
      val process = allProcesses[pid] ?: return@forEach
      val jvmProcessNode = JvmProcessNode(process, vmDescriptor)

      val isTopLevel = process.parentProcessID == 1 || process.parentProcessID == 0
      if (isTopLevel) {
        topLevelJvmProcessNodes.add(jvmProcessNode)
      }
      else {
        remainingJvmProcessNodesToAssociate[pid] = jvmProcessNode
      }
    }

    // Collect all children of the real top-level Java processes.
    progressIndicator.checkCanceled()
    topLevelJvmProcessNodes.forEach { javaProcessNode ->
      collectChildren(allProcesses, javaProcessNode, remainingJvmProcessNodesToAssociate)
    }

    // The remaining processes in `remainingJavaProcessNodesToAssociate` are
    // not a top-level process but there are also not a child process of a
    // top-level Java process. We add these as a top level process to the result
    // so that we have a listing of all Java processes.
    progressIndicator.checkCanceled()
    while (remainingJvmProcessNodesToAssociate.isNotEmpty()) {
      val (pid, javaProcessNode) = remainingJvmProcessNodesToAssociate.first()
      remainingJvmProcessNodesToAssociate.remove(pid)
      collectChildren(allProcesses, javaProcessNode, remainingJvmProcessNodesToAssociate)
      topLevelJvmProcessNodes.add(javaProcessNode)
    }

    topLevelJvmProcessNodes.sortWith { a, b -> a.process.processID.compareTo(b.process.processID) }

    LOG.debug("Took " + (System.currentTimeMillis() - startMillis) + " ms to collect all Java processes.")
  }

  override fun onSuccess() {
    onSuccess(topLevelJvmProcessNodes)
  }

  override fun onFinished() {
    onFinished.invoke()
  }

  override fun onThrowable(error: Throwable) {
    onThrowable.invoke(error)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun collectChildren(processes: Map<Int, OSProcess>, parent: ProcessNode, javaProcessesToAssociate: MutableMap<Int, JvmProcessNode>) {
    processes.filter { it.value.parentProcessID == parent.process.processID }.map { childProcess ->
      val childProcessNode = javaProcessesToAssociate.remove(childProcess.value.processID) ?: ProcessNode(childProcess.value)
      collectChildren(processes, childProcessNode, javaProcessesToAssociate)
      childProcessNode
    }.sortedBy { childProcessNode -> childProcessNode.process.processID }.forEach { parent.add(it) }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}