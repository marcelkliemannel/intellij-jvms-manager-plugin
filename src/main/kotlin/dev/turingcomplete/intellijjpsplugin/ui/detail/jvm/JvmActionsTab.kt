package dev.turingcomplete.intellijjpsplugin.ui.detail.jvm

import com.intellij.icons.AllIcons
import com.intellij.ide.plugins.newui.HorizontalLayout
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkType
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.ui.GuiUtils
import com.intellij.ui.SeparatorWithText
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.JvmAction
import dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.JvmActionContext
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjpsplugin.ui.common.overrideTopInset
import dev.turingcomplete.intellijjpsplugin.ui.detail.ProcessDetailTab
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class JvmActionsTab(val project: Project, processNode: ProcessNode)
  : ProcessDetailTab("JVM Actions", processNode) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent() = JvmActionsPanel(project, processNode)

  override fun processNodeUpdated() {
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class JvmActionsPanel(project: Project, processNode: ProcessNode) : JPanel(GridBagLayout()), DataProvider {

    private val sdksModel = ProjectSdksModel().apply { syncSdks() }
    private val jdkComboBox = JdkComboBox(project, sdksModel, { it is JavaSdkType }, null, null, null)
    private val jvmActionContext = JvmActionContext(project, { jdkComboBox.selectedJdk }, processNode)

    init {
      border = EmptyBorder(UIUtil.PANEL_REGULAR_INSETS)

      val bag = UiUtils.createDefaultGridBag()

      val jvmActionsWrapper = JvmActionsWrapper()

      add(JBLabel("Use tools from JDK:"), bag.nextLine().next().weightx(1.0).fillCellHorizontally())
      add(JPanel(HorizontalLayout(UIUtil.DEFAULT_HGAP)).apply {
        add(jdkComboBox)
        add(JLabel(AllIcons.General.ContextHelp).apply {
          toolTipText = "<html>Some of the following actions will use the executables from the <code>bin</code> " +
                        "directory of the selected JDK.<br><br>The availability of the actions depends on the " +
                        "version of the JDK and its vendor.</html>"
        })
      }, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP / 2))

      syncJvmActionsWrapper(jvmActionsWrapper)
      jdkComboBox.addActionListener { syncJvmActionsWrapper(jvmActionsWrapper) }

      add(jvmActionsWrapper, bag.nextLine().next().weightx(1.0).fillCellHorizontally())

      // Fill rest of panel
      add(JPanel(), bag.nextLine().next().weightx(1.0).weighty(1.0).fillCell())
    }

    override fun getData(dataId: String) = when {
      JvmActionContext.DATA_KEY.`is`(dataId) -> jvmActionContext
      else -> null
    }

    private fun syncJvmActionsWrapper(jvmActionsWrapper: JComponent) {
      GuiUtils.enableChildren(jdkComboBox.selectedJdk != null, jvmActionsWrapper)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class JvmActionsWrapper : JPanel(GridBagLayout()) {

    init {
      border = UiUtils.EMPTY_BORDER

      val bag = UiUtils.createDefaultGridBag()

      JvmAction.EP.extensions.forEach { jvmAction ->
        add(createSeparator(jvmAction), bag.nextLine().next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).fillCellHorizontally())
        add(jvmAction.createComponent(), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP).weightx(1.0).fillCellHorizontally())
      }
    }

    private fun createSeparator(jvmAction: JvmAction) = SeparatorWithText().apply {
      caption = jvmAction.title
      setCaptionCentered(false)
    }
  }
}