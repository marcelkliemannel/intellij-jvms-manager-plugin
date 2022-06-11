package dev.turingcomplete.intellijjpsplugin.ui.detail.jvmaction

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode

class JvmActionContext(val project: Project, val jdk: () -> Sdk?, val processNode: ProcessNode) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}