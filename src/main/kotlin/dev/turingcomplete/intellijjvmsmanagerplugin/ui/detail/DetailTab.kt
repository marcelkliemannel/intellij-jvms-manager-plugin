package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail

import com.intellij.openapi.Disposable
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode
import javax.swing.JComponent
import kotlin.properties.Delegates

abstract class DetailTab<T : ProcessNode>(val title: String, initialProcessNode: T) {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //

  var processNode: T by Delegates.observable(initialProcessNode) { _, _, _ -> processNodeUpdated() }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  abstract fun createComponent(parent: Disposable): JComponent

  abstract fun processNodeUpdated()

  // -- Inner Type ---------------------------------------------------------- //
}
