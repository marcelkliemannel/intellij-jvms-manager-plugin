package dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction

import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode

class JvmActionContext(val project: Project,
                       val jdk: () -> Sdk?,
                       val processNode: ProcessNode) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    val DATA_KEY: DataKey<JvmActionContext> = DataKey.create("JavaProcessesPlugin.JToolActionContext")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}