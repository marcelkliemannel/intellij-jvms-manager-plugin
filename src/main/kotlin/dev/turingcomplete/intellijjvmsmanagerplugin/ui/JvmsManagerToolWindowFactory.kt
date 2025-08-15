package dev.turingcomplete.intellijjvmsmanagerplugin.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import com.intellij.ui.content.ContentFactory
import dev.turingcomplete.intellijjvmsmanagerplugin.JvmsManagerPluginService
import dev.turingcomplete.intellijjvmsmanagerplugin.settings.JvmsManagerSettingsService

class JvmsManagerToolWindowFactory : ToolWindowFactory, DumbAware, Disposable {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    private const val TOOL_WINDOW_ID = "JVMsManager"
    const val TOOLBAR_PLACE_PREFIX = "dev.turingcomplete.intellijjvmsmanagerplugin.place"

    fun <T> getData(dataProvider: DataProvider, dataKey: DataKey<T>): Any? {
      val project = CommonDataKeys.PROJECT.getData(dataProvider) ?: return null
      val toolWindow =
        ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return null

      val toolWindowContentComponent = toolWindow.contentManager.selectedContent?.component
      return if (toolWindowContentComponent is DataProvider)
        dataKey.getData(toolWindowContentComponent)
      else null
    }

    fun getJvmProcessesMainPanel(project: Project): JvmProcessesMainPanel? {
      val toolWindow =
        ToolWindowManager.getInstance(project).getToolWindow(TOOL_WINDOW_ID) ?: return null
      val toolWindowContentComponent = toolWindow.contentManager.selectedContent?.component
      return if (toolWindowContentComponent is JvmProcessesMainPanel) toolWindowContentComponent
      else null
    }
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
    Disposer.register(toolWindow.disposable, this)

    ApplicationManager.getApplication().invokeLater {
      val jvmProcessesMainPanel = JvmProcessesMainPanel(project, toolWindow.disposable)
      ContentFactory.getInstance().createContent(jvmProcessesMainPanel, null, false).apply {
        putUserData(ToolWindow.SHOW_CONTENT_ICON, false)
        isCloseable = false
        toolWindow.contentManager.addContent(this)
      }
      // The event on the message bus will not be triggered if the tool window
      // is already open during the start of the tool window.
      if (toolWindow.isVisible) {
        collectJvmProcessesOnToolWindowOpen(project)
      }
    }

    project.messageBus
      .connect(toolWindow.disposable)
      .subscribe(
        ToolWindowManagerListener.TOPIC,
        object : ToolWindowManagerListener {

          override fun toolWindowShown(toolWindow: ToolWindow) {
            if (toolWindow.id == TOOL_WINDOW_ID) {
              collectJvmProcessesOnToolWindowOpen(project)
            }
          }
        },
      )
  }

  override fun init(toolWindow: ToolWindow) {
    assert(toolWindow.id == TOOL_WINDOW_ID)

    toolWindow.component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "false")
    toolWindow.stripeTitle = "JVMs"
  }

  override fun dispose() {}

  // -- Private Methods ----------------------------------------------------- //

  private fun collectJvmProcessesOnToolWindowOpen(project: Project) {
    if (!JvmsManagerSettingsService.getInstance().collectJvmProcessesOnToolWindowOpen) {
      return
    }

    project.getService(JvmsManagerPluginService::class.java).collectJavaProcesses()
  }

  // -- Inner Type ---------------------------------------------------------- //
}
