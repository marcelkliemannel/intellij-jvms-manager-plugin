package dev.turingcomplete.intellijjpsplugin.ui.detail

import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import javax.swing.Icon
import javax.swing.JComponent

abstract class ProcessDetailTab(val title: String, protected var processNode: ProcessNode, val icon: Icon? = null) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  abstract fun createComponent(): JComponent

  fun showProcessNode(processNode: ProcessNode) {
    this.processNode = processNode

    processNodeUpdated()
  }

  abstract fun processNodeUpdated()

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}