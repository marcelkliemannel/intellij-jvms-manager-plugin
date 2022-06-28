package dev.turingcomplete.intellijjpsplugin.settings

import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.FormBuilder
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import javax.swing.JComponent
import javax.swing.JPanel

internal class JpsSettingsComponent {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val jvmActionsJdkComboBox = JdkComboBox(null, JpsSettingsService.getInstance().getSdksModel(),
                                                  { it is JavaSdkType }, null, null, null)

  private var mainPanel: JPanel = FormBuilder.createFormBuilder()
          .addLabeledComponent(JBLabel("JDK used for JVM actions: "), jvmActionsJdkComboBox, 1, true)
          .addComponent(JBLabel("<html>Some of the JVM action will use the executables from the <code>bin</code> " +
                                "directory of the selected JDK.<br>The availability of the actions depends on the " +
                                "version of the JDK and its vendor.</html>", UIUtil.ComponentStyle.SMALL), 0)
          .addComponentFillVertically(UiUtils.EMPTY_FILL_PANEL(), 0)
          .panel

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun getPanel(): JPanel = mainPanel

  fun getPreferredFocusedComponent(): JComponent = jvmActionsJdkComboBox

  fun setSelectedJvmActionsJdk(jdkName: String?) {
    if (jdkName == null) {
      jvmActionsJdkComboBox.selectedJdk = null
      return
    }

    val selectedModuleJdk: Sdk? = JpsSettingsService.getInstance().getSdksModel().findSdk(jdkName)
    if (selectedModuleJdk != null) {
      jvmActionsJdkComboBox.selectedJdk = selectedModuleJdk
    }
    else {
      jvmActionsJdkComboBox.setInvalidJdk(jdkName)
    }
  }

  fun getSelectedJvmActionsJdk(): Sdk? = jvmActionsJdkComboBox.selectedJdk

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}