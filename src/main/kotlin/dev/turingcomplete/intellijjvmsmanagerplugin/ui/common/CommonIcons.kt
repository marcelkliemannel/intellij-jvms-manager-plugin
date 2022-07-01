package dev.turingcomplete.intellijjvmsmanagerplugin.ui.common

import com.intellij.ui.IconManager
import javax.swing.Icon

object CommonIcons {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val JVM: Icon = loadIcon("jvm.svg")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun loadIcon(fileName: String): Icon {
    return IconManager.getInstance().getIcon("dev/turingcomplete/intellijjvmsmanagerplugin/icons/$fileName", CommonIcons::class.java)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}