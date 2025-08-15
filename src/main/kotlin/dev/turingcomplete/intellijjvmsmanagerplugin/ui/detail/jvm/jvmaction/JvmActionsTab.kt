package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.IdeBorderFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjvmsmanagerplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.overrideBottomInset
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.overrideTopInset
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.DetailTab
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.ProcessDescriptionPanel
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

class JvmActionsTab(val project: Project, initialProcessNode: JvmProcessNode) :
  DetailTab<JvmProcessNode>("JVM Actions", initialProcessNode) {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //

  private lateinit var jvmActionsPanel: JvmActionsPanel

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun createComponent(parent: Disposable): JComponent {
    jvmActionsPanel = JvmActionsPanel(parent)
    return jvmActionsPanel
  }

  override fun processNodeUpdated() {
    jvmActionsPanel.newProcessNodeSet()
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  inner class JvmActionsPanel(parent: Disposable) : JPanel(GridBagLayout()), DataProvider {

    private val processDescriptionPanel = ProcessDescriptionPanel(project)
    private lateinit var jvmActionContext: JvmActionContext

    init {
      border = JBUI.Borders.empty(UIUtil.PANEL_REGULAR_INSETS)

      val bag = UiUtils.createDefaultGridBag()

      add(
        processDescriptionPanel,
        bag
          .nextLine()
          .next()
          .weightx(1.0)
          .overrideTopInset(UIUtil.DEFAULT_HGAP / 2)
          .fillCellHorizontally(),
      )
      add(
        JvmActionsWrapper(project, parent),
        bag
          .nextLine()
          .next()
          .weightx(1.0)
          .overrideTopInset(UIUtil.DEFAULT_HGAP)
          .fillCellHorizontally(),
      )

      // Fill rest of panel
      add(
        UiUtils.EMPTY_FILL_PANEL(),
        bag
          .nextLine()
          .next()
          .weightx(1.0)
          .overrideBottomInset(UIUtil.DEFAULT_HGAP / 2)
          .weighty(1.0)
          .fillCell(),
      )

      newProcessNodeSet()
    }

    override fun getData(dataId: String) =
      when {
        JvmActionContext.DATA_KEY.`is`(dataId) -> jvmActionContext
        else -> null
      }

    fun newProcessNodeSet() {
      jvmActionContext = JvmActionContext(project, processNode)
      processDescriptionPanel.processNodeUpdated(processNode)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class JvmActionsWrapper(project: Project, parent: Disposable) : JPanel(GridBagLayout()) {

    init {
      border = UiUtils.EMPTY_BORDER

      val bag = UiUtils.createDefaultGridBag()

      val jvmActions = JvmAction.EP.extensions
      for (i in jvmActions.indices) {
        val jvmAction = jvmActions[i]
        val component =
          BorderLayoutPanel().apply {
            border = IdeBorderFactory.createTitledBorder(jvmAction.title, false)
            addToCenter(jvmAction.createComponent(project, parent))
          }
        add(component, bag.nextLine().next().weightx(1.0).fillCellHorizontally())
      }
    }
  }
}
