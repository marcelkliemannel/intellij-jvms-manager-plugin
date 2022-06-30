package dev.turingcomplete.intellijjpsplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "dev.turingcomplete.intellijjpsplugin.settings.JpsSettingsService",
       storages = [Storage("JpsSettingsPlugin.xml")])
class JpsSettingsService : PersistentStateComponent<JpsSettingsService> {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    fun getInstance(): JpsSettingsService {
      return ApplicationManager.getApplication().getService(JpsSettingsService::class.java)
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val sdksModel = ProjectSdksModel().apply { syncSdks() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  var collectJvmProcessesOnToolWindowOpen: Boolean = true
  var jvmActionsJdkName: String? = null

  override fun getState(): JpsSettingsService {
    return this
  }

  fun getSdksModel(): ProjectSdksModel = sdksModel

  fun getJvmActionJdk(): Sdk? = jvmActionsJdkName?.let { sdksModel.findSdk(it) }

  override fun loadState(state: JpsSettingsService) {
    XmlSerializerUtil.copyBean(state, this)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}