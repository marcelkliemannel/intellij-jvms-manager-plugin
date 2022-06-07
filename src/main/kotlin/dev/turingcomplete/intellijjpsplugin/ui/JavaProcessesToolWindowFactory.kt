package dev.turingcomplete.intellijjpsplugin.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory

class JavaProcessesToolWindowFactory : ToolWindowFactory, DumbAware, Disposable {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    const val PLACE_PREFIX = "dev.turingcomplete.intellijjpsplugin.place"
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    Disposer.register(toolWindow.disposable, this)

    val contentManager = toolWindow.contentManager
    val content = contentManager.factory.createContent(JavaProcessesPanel(project), "All", true)
    contentManager.addContent(content)
  }

  override fun dispose() {
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}