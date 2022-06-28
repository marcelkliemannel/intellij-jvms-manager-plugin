package dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.jvmaction

import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjpsplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjpsplugin.ui.common.overrideTopInset
import dev.turingcomplete.intellijjpsplugin.ui.detail.DetailTab
import dev.turingcomplete.intellijjpsplugin.ui.detail.ProcessDescriptionPanel
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class JvmActionsTab(val project: Project, initialProcessNode: JvmProcessNode)
  : DetailTab<JvmProcessNode>("JVM Actions", initialProcessNode) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var jvmActionsPanel: JvmActionsPanel

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent(): JComponent {
    jvmActionsPanel = JvmActionsPanel()
    return jvmActionsPanel
  }

  override fun processNodeUpdated() {
    jvmActionsPanel.newProcessNodeSet()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  inner class JvmActionsPanel : JPanel(GridBagLayout()), DataProvider {

    private val processDescriptionPanel = ProcessDescriptionPanel(project)
    private lateinit var jvmActionContext: JvmActionContext

    init {
      border = EmptyBorder(UIUtil.PANEL_REGULAR_INSETS)

      val bag = UiUtils.createDefaultGridBag()

      add(processDescriptionPanel, bag.nextLine().next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())
      add(JvmActionsWrapper(), bag.nextLine().next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      // Fill rest of panel
      add(JPanel(), bag.nextLine().next().weightx(1.0).weighty(1.0).fillCell())

      newProcessNodeSet()
    }

    override fun getData(dataId: String) = when {
      JvmActionContext.DATA_KEY.`is`(dataId) -> jvmActionContext
      else -> null
    }

    fun newProcessNodeSet() {
      jvmActionContext = JvmActionContext(project, processNode)
      processDescriptionPanel.processNodeUpdated(processNode)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class JvmActionsWrapper : JPanel(GridBagLayout()) {

    init {
      border = UiUtils.EMPTY_BORDER

      val bag = UiUtils.createDefaultGridBag()

      val jvmActions = JvmAction.EP.extensions
      for (i in jvmActions.indices) {
        val jvmAction = jvmActions[i]
        val topInset = if (i == 0) 0 else UIUtil.DEFAULT_HGAP
        add(UiUtils.createSeparator(jvmAction.title), bag.nextLine().next().weightx(1.0).overrideTopInset(topInset).fillCellHorizontally())
        add(jvmAction.createComponent(), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP).weightx(1.0).fillCellHorizontally())
      }
    }
  }
}