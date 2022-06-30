package dev.turingcomplete.intellijjpsplugin.settings

import com.intellij.openapi.options.Configurable
import java.util.*
import javax.swing.JComponent

internal class JpsSettingsConfigurable : Configurable {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  private var settingsComponent: JpsSettingsComponent? = null

  override fun getDisplayName(): String {
    return "JPS"
  }

  override fun getPreferredFocusedComponent(): JComponent {
    return settingsComponent!!.getPreferredFocusedComponent()
  }

  override fun createComponent(): JComponent {
    settingsComponent = JpsSettingsComponent()
    return settingsComponent!!.getPanel()
  }

  override fun isModified(): Boolean {
    val settings: JpsSettingsService = JpsSettingsService.getInstance()
    return !Objects.equals(settings.jvmActionsJdkName, settingsComponent!!.getSelectedJvmActionsJdk())
           || settings.collectJvmProcessesOnToolWindowOpen != settingsComponent!!.getCollectJvmProcessesOnToolWindowOpen()
  }

  override fun apply() {
    val settings: JpsSettingsService = JpsSettingsService.getInstance()
    settingsComponent!!.also {
      settings.jvmActionsJdkName = it.getSelectedJvmActionsJdk()?.name
      settings.collectJvmProcessesOnToolWindowOpen = it.getCollectJvmProcessesOnToolWindowOpen()
    }
  }

  override fun reset() {
    val settings: JpsSettingsService = JpsSettingsService.getInstance()
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