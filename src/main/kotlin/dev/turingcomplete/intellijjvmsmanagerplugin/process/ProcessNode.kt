package dev.turingcomplete.intellijjvmsmanagerplugin.process

import com.intellij.openapi.application.ApplicationManager
import oshi.PlatformEnum
import oshi.software.os.OSProcess
import javax.swing.tree.DefaultMutableTreeNode

open class ProcessNode(val process: OSProcess) : DefaultMutableTreeNode() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private const val TERMINATION_TRIGGERED_WARNING_TEXT = "<html>Termination of this process was triggered.</html>"
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  val processType: ProcessType by lazy { determineProcessType() }
  val smartName: String by lazy { determineSmartName() }
  var collectedAtMillis: Long = System.currentTimeMillis()
    private set
  var warningText: String? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  protected open fun determineProcessType(): ProcessType = ProcessType.determine(process, null)

  protected open fun determineSmartName() : String {
    val processName = process.name
    if (processName.isNotBlank()) {
      return processName
    }

    return "Unknown"
  }

  fun processDescription(): String {
    return when (processType) {
      ProcessType.UNKNOWN -> process.name
      else -> "${processType.description} (${process.name})"
    }
  }

  fun priorityDescription(): String? {
    return when (OshiUtils.CURRENT_PLATFORM) {
      PlatformEnum.MACOS -> "Priority on macOS ranges from 0 (lowest) to 127 (highest).\nThe default priority is 31."
      PlatformEnum.LINUX -> "Priority on Linux ranges from -20 (highest) to 19/20 (lowest).\nThe default priority is 0."
      PlatformEnum.WINDOWS -> "Priority on Windows ranges from 0 (lowest) to 32 (highest)."
      else -> null
    }
  }

  override fun toString(): String = process.processID.toString()

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }

    if (javaClass != other?.javaClass){
      return false
    }

    other as ProcessNode
    return process.processID == other.process.processID
  }

  override fun hashCode(): Int {
    return process.processID
  }

  fun update() {
    assert(!ApplicationManager.getApplication().isDispatchThread)

    val attributesUpdated: Boolean = process.updateAttributes()
    if (!attributesUpdated) {
      warningText = "<html>Failed to update process information.<br>The process was probably terminated.</html>"
    }
    else {
      warningText = null
      collectedAtMillis = System.currentTimeMillis()
    }
  }

  fun terminateGracefully() {
    ProcessHandle.of(process.processID.toLong()).ifPresent { it.destroy() }
    warningText = TERMINATION_TRIGGERED_WARNING_TEXT
  }

  fun terminateForcibly() {
    ProcessHandle.of(process.processID.toLong()).ifPresent { it.destroyForcibly() }
    warningText = TERMINATION_TRIGGERED_WARNING_TEXT
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}