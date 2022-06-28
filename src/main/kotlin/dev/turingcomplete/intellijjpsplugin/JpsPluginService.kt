package dev.turingcomplete.intellijjpsplugin

import com.intellij.openapi.components.Service
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.settings.JpsSettingsConfigurable
import dev.turingcomplete.intellijjpsplugin.ui.JpsToolWindowFactory

@Service(Service.Level.PROJECT)
class JpsPluginService(val project: Project) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun processDetailsUpdated(processNodes: List<ProcessNode>) {
    JpsToolWindowFactory.getJavaProcessesMainPanel(project)?.processDetailsUpdated(processNodes)
  }

  fun showProcessDetails(processNode: ProcessNode) {
    JpsToolWindowFactory.getJavaProcessesMainPanel(project)?.showProcessDetails(processNode)
  }

  fun collectJavaProcesses() {
    JpsToolWindowFactory.getJavaProcessesMainPanel(project)?.collectJvmProcesses()
  }

  fun showSettings() {
    ShowSettingsUtil.getInstance().showSettingsDialog(project, JpsSettingsConfigurable::class.java)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}