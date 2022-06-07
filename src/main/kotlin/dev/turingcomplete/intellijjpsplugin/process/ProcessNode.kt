package dev.turingcomplete.intellijjpsplugin.process

import oshi.PlatformEnum
import oshi.SystemInfo
import oshi.software.os.OSProcess
import javax.swing.tree.DefaultMutableTreeNode

open class ProcessNode(val process: OSProcess) : DefaultMutableTreeNode() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val processType: ProcessType by lazy { determineProcessType() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  protected open fun determineProcessType(): ProcessType = ProcessType.determine(process, null)

  open fun displayName(): String {
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

  fun stateDescription(): String {
    return when(process.state) {
      OSProcess.State.NEW -> "Process is in the creation phase"
      OSProcess.State.RUNNING -> "Process is in execution"
      OSProcess.State.SLEEPING -> "Process is in an interruptible sleep state"
      OSProcess.State.WAITING -> "Process is in a blocked, uninterruptible sleep state"
      OSProcess.State.ZOMBIE -> "Process is in termination phase"
      OSProcess.State.STOPPED -> "Process was stopped by the user (e.g., for debugging)"
      OSProcess.State.INVALID -> "Process is no longer valid, probably due to termination"
      OSProcess.State.SUSPENDED -> "Waiting if the process has been intentionally suspended"
      // handle OSProcess.State.OTHER as else
      else -> "Process is in an unknown or undefined state"
    }
  }

  fun priorityDescription(): String? {
    return when (SystemInfo.getCurrentPlatform()) {
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

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}