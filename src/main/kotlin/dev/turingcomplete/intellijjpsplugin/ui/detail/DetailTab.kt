package dev.turingcomplete.intellijjpsplugin.ui.detail

import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import javax.swing.JComponent
import kotlin.properties.Delegates

abstract class DetailTab<T : ProcessNode>(val title: String, initialProcessNode: T) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  var processNode: T by Delegates.observable(initialProcessNode) { _, _, _ -> processNodeUpdated() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  abstract fun createComponent(): JComponent

  protected abstract fun processNodeUpdated()

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}