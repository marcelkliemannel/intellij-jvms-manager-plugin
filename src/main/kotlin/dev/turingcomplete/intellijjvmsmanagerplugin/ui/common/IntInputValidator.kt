package dev.turingcomplete.intellijjvmsmanagerplugin.ui.common

import com.intellij.openapi.ui.InputValidator

class IntInputValidator private constructor() : InputValidator {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    val INSTANCE = IntInputValidator()
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun checkInput(inputString: String?): Boolean = inputString?.toIntOrNull() != null

  override fun canClose(inputString: String?): Boolean = true

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
