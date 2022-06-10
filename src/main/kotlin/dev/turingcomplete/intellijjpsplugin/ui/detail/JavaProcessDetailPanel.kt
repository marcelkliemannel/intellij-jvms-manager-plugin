package dev.turingcomplete.intellijjpsplugin.ui.detail

import dev.turingcomplete.intellijjpsplugin.process.JavaProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode

class JavaProcessDetailPanel(javaProcessNode: JavaProcessNode, showParentProcessDetails: (ProcessNode) -> Unit)
  : ProcessDetailPanel<JavaProcessNode>(javaProcessNode, showParentProcessDetails) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}