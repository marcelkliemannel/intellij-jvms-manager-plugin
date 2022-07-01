package dev.turingcomplete.intellijjvmsmanagerplugin.process

import oshi.PlatformEnum
import oshi.SystemInfo
import oshi.software.os.OSProcess
import oshi.software.os.OperatingSystem

object OshiUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val OPERATION_SYSTEM: OperatingSystem by lazy { SystemInfo().operatingSystem }
  val CURRENT_PLATFORM: PlatformEnum by lazy { SystemInfo.getCurrentPlatform() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}

fun OSProcess.State.stateDescription(): String {
  return when(this) {
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