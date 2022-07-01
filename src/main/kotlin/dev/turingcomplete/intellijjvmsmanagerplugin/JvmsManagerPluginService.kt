package dev.turingcomplete.intellijjvmsmanagerplugin

import com.intellij.openapi.components.Service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.settings.JvmsManagerSettingsConfigurable
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.JvmsManagerToolWindowFactory

@Service(Service.Level.PROJECT)
class JvmsManagerPluginService(val project: Project) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun processDetailsUpdated(processNodes: List<ProcessNode>) {
    JvmsManagerToolWindowFactory.getJvmProcessesMainPanel(project)?.processDetailsUpdated(processNodes)
  }

  fun showProcessDetails(processNode: ProcessNode) {
    JvmsManagerToolWindowFactory.getJvmProcessesMainPanel(project)?.showProcessDetails(processNode)
  }

  fun collectJavaProcesses(onlyIfNoProcesses: Boolean = false) {
    JvmsManagerToolWindowFactory.getJvmProcessesMainPanel(project)?.collectJvmProcesses(onlyIfNoProcesses)
  }

  fun showSettings() {
    ShowSettingsUtil.getInstance().showSettingsDialog(project, JvmsManagerSettingsConfigurable::class.java)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}