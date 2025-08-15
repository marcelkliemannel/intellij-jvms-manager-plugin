package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction

import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import javax.swing.JComponent

abstract class JvmAction(val title: String) {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    val EP: ExtensionPointName<JvmAction> =
      ExtensionPointName.create("dev.turingcomplete.intellijjvmsmanagerplugin.jvmAction")
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  abstract fun createComponent(project: Project, parent: Disposable): JComponent

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
