package dev.turingcomplete.intellijjpsplugin.jps

import oshi.software.os.OSProcess
import javax.swing.tree.DefaultMutableTreeNode

open class ProcessNode(val process: OSProcess) : DefaultMutableTreeNode() {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val processType: ProcessType? by lazy { determineProcessType() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  protected open fun determineProcessType(): ProcessType? = ProcessType.determine(process, null)

  open fun displayName(): String {
    val processName = process.name
    if (processName.isNotBlank()) {
      return processName
    }

    return "Unknown"
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