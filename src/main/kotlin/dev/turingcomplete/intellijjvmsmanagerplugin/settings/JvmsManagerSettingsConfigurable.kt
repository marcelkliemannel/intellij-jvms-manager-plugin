package dev.turingcomplete.intellijjvmsmanagerplugin.settings

import com.intellij.openapi.options.Configurable
import java.util.*
import javax.swing.JComponent

class JvmsManagerSettingsConfigurable : Configurable {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  private var settingsComponent: JvmsManagerSettingsComponent? = null

  override fun getDisplayName(): String {
    return "JVMs Manager"
  }

  override fun getPreferredFocusedComponent(): JComponent {
    return settingsComponent!!.getPreferredFocusedComponent()
  }

  override fun createComponent(): JComponent {
    settingsComponent = JvmsManagerSettingsComponent()
    return settingsComponent!!.getPanel()
  }

  override fun isModified(): Boolean {
    val settings: JvmsManagerSettingsService = JvmsManagerSettingsService.getInstance()
    return !Objects.equals(settings.jvmActionsJdkName, settingsComponent!!.getSelectedJvmActionsJdk())
           || settings.collectJvmProcessesOnToolWindowOpen != settingsComponent!!.getCollectJvmProcessesOnToolWindowOpen()
  }

  override fun apply() {
    val settings: JvmsManagerSettingsService = JvmsManagerSettingsService.getInstance()
    settingsComponent!!.also {
      settings.jvmActionsJdkName = it.getSelectedJvmActionsJdk()?.name
      settings.collectJvmProcessesOnToolWindowOpen = it.getCollectJvmProcessesOnToolWindowOpen()
    }
  }

  override fun reset() {
    val settings: JvmsManagerSettingsService = JvmsManagerSettingsService.getInstance()
    settingsComponent!!.also {
      it.setSelectedJvmActionsJdk(settings.jvmActionsJdkName)
      it.setCollectJvmProcessesOnToolWindowOpen(settings.collectJvmProcessesOnToolWindowOpen)
    }
  }

  override fun disposeUIResources() {
    settingsComponent = null
  }


  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}