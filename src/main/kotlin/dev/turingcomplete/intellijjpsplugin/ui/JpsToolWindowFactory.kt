package dev.turingcomplete.intellijjpsplugin.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.ContentFactory
import com.intellij.util.castSafelyTo
import dev.turingcomplete.intellijjpsplugin.JpsPluginService

class JpsToolWindowFactory : ToolWindowFactory, DumbAware, Disposable {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private const val TOOL_WINDOW_ID = "JPS"
    const val TOOLBAR_PLACE_PREFIX = "dev.turingcomplete.intellijjpsplugin.place"

    fun <T> getData(dataProvider: DataProvider, dataKey: DataKey<T>): Any? {
      val project = dataProvider.getData(CommonDataKeys.PROJECT.name).castSafelyTo<Project>() ?: return null
      val jpsToolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return null

      val toolWindowContentComponent = jpsToolWindow.contentManager.selectedContent?.component
      return if (toolWindowContentComponent is DataProvider) toolWindowContentComponent.getData(dataKey.name) else null
    }

    fun getJavaProcessesMainPanel(project: Project): JvmProcessesMainPanel? {
      val jpsToolWindow = ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return null
      val toolWindowContentComponent = jpsToolWindow.contentManager.selectedContent?.component
      return if (toolWindowContentComponent is JvmProcessesMainPanel) toolWindowContentComponent else null
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    Disposer.register(toolWindow.disposable, this)

    val mainContent = ContentFactory.SERVICE.getInstance().createContent(JvmProcessesMainPanel(project), null, false)
    mainContent.putUserData(ToolWindow.SHOW_CONTENT_ICON, false)
    mainContent.isCloseable = false

    toolWindow.apply {
      component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "false")
      contentManager.addContent(mainContent)

      show { project.getService(JpsPluginService::class.java).collectJavaProcesses() }
    }
  }

  override fun init(toolWindow: ToolWindow) {
    toolWindow.stripeTitle = "JPS"
    toolWindow.isShowStripeButton = false
  }

  override fun dispose() {
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}