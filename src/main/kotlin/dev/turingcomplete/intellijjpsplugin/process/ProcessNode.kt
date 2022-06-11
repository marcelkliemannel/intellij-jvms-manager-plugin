package dev.turingcomplete.intellijjpsplugin.process

import oshi.PlatformEnum
import oshi.software.os.OSProcess
import javax.swing.tree.DefaultMutableTreeNode

open class ProcessNode(val process: OSProcess) : DefaultMutableTreeNode() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val processType: ProcessType by lazy { determineProcessType() }
  val smartName: String by lazy { determineSmartName() }

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

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}