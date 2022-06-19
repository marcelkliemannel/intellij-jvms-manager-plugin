package dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction

import com.intellij.openapi.extensions.ExtensionPointName
import javax.swing.JComponent

abstract class JvmAction(val title: String) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    val EP: ExtensionPointName<JvmAction> = ExtensionPointName.create("dev.turingcomplete.intellijjpsplugin.jvmAction")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  abstract fun createComponent(): JComponent

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}