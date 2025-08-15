package dev.turingcomplete.intellijjvmsmanagerplugin.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
  name = "dev.turingcomplete.intellijjvmsmanagerplugin.settings.JvmsManagerSettingsService",
  storages = [Storage("JvmsManagerSettingsPlugin.xml")],
)
class JvmsManagerSettingsService : PersistentStateComponent<JvmsManagerSettingsService> {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    fun getInstance(): JvmsManagerSettingsService {
      return ApplicationManager.getApplication().getService(JvmsManagerSettingsService::class.java)
    }
  }

  // -- Properties ---------------------------------------------------------- //

  private val sdksModel = ProjectSdksModel().apply { syncSdks() }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  var collectJvmProcessesOnToolWindowOpen: Boolean = true
  var jvmActionsJdkName: String? = null

  override fun getState(): JvmsManagerSettingsService {
    return this
  }

  fun getSdksModel(): ProjectSdksModel = sdksModel

  fun getJvmActionJdk(): Sdk? = jvmActionsJdkName?.let { sdksModel.findSdk(it) }

  override fun loadState(state: JvmsManagerSettingsService) {
    XmlSerializerUtil.copyBean(state, this)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
