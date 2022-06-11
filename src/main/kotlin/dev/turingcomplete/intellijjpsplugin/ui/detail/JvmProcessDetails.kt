package dev.turingcomplete.intellijjpsplugin.ui.detail

import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel
import com.intellij.ui.GuiUtils
import com.intellij.ui.SeparatorWithText
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjpsplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.common.CommonIcons
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjpsplugin.ui.common.overrideTopInset
import dev.turingcomplete.intellijjpsplugin.ui.detail.jvmaction.JvmAction
import dev.turingcomplete.intellijjpsplugin.ui.detail.jvmaction.JvmActionContext
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class JvmProcessDetails(private val project: Project,
                        jvmProcessNode: JvmProcessNode,
                        showParentProcessDetails: (ProcessNode) -> Unit)
  : ProcessDetails<JvmProcessNode>(jvmProcessNode, showParentProcessDetails) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createAdditionalTabs(): Sequence<ProcessDetailTab> {
    return sequenceOf(JvmActionsPanel(project, processNode))
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class JvmActionsPanel(project: Project, processNode: ProcessNode)
    : ProcessDetailTab("JVM Actions", processNode, CommonIcons.JAVA) {

    private val sdkModel = ProjectSdksModel()
    private val jdkComboBox = JdkComboBox(project, sdkModel, { it.equals(JavaSdk.getInstance()) }, null, null, null)
    private val jvmActionContext = JvmActionContext(project, { jdkComboBox.selectedJdk }, processNode)

    override fun createComponent() = JPanel(GridBagLayout()).apply {
      border = EmptyBorder(UIUtil.PANEL_REGULAR_INSETS)

      val bag = UiUtils.createDefaultGridBag()

      val jvmActionComponentsWrapper = createJvmActionComponentsWrapper()

      add(JBLabel("Use tools from JDK:"), bag.nextLine().next().weightx(1.0).fillCellHorizontally())
      add(jdkComboBox, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP / 2))
      jdkComboBox.addActionListener {
        GuiUtils.enableChildren(jdkComboBox.selectedJdk != null, jvmActionComponentsWrapper)
      }

      add(jvmActionComponentsWrapper, bag.nextLine().next().weightx(1.0).fillCellHorizontally())

      // Fill rest of panel
      add(JPanel(), bag.nextLine().next().weightx(1.0).weighty(1.0).fillCell())
    }

    private fun createJvmActionComponentsWrapper(): JComponent {
      return JPanel(GridBagLayout()).apply {
        border = UiUtils.EMPTY_BORDER

        val bag = UiUtils.createDefaultGridBag()

        JvmAction.EP.extensions.forEach { jvmAction ->
          add(SeparatorWithText().apply {
            caption = jvmAction.title
            setCaptionCentered(false)
          }, bag.nextLine().next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).fillCellHorizontally())
          add(jvmAction.createComponent(jvmActionContext), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP).weightx(1.0).fillCellHorizontally())
        }

        GuiUtils.enableChildren(jdkComboBox.selectedJdk != null, this)
      }
    }

    override fun processNodeUpdated() {
    }
  }
}