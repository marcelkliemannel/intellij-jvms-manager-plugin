package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction

import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode

class JvmActionContext(val project: Project, val processNode: ProcessNode) {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    val DATA_KEY: DataKey<JvmActionContext> =
      DataKey.create("dev.turingcomplete.intellijjvmsmanagerplugin.jvmActionContext")
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
