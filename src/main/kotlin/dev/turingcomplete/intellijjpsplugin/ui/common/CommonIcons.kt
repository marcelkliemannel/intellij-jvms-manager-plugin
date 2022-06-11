package dev.turingcomplete.intellijjpsplugin.ui.common

import com.intellij.ui.IconManager
import javax.swing.Icon

object CommonIcons {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val JAVA: Icon = loadIcon("java.svg")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun loadIcon(fileName: String): Icon {
    return IconManager.getInstance().getIcon("dev/turingcomplete/intellijjpsplugin/icons/$fileName", CommonIcons::class.java)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}