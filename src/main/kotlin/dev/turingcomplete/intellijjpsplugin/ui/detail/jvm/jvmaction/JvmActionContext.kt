package dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.jvmaction

import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode

class JvmActionContext(val project: Project, val processNode: ProcessNode) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    val DATA_KEY: DataKey<JvmActionContext> = DataKey.create("dev.turingcomplete.intellijjpsplugin.jvmActionContext")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}