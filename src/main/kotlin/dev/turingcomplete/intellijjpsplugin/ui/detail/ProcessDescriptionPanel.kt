package dev.turingcomplete.intellijjpsplugin.ui.detail

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.JpsToolWindowFactory
import dev.turingcomplete.intellijjpsplugin.ui.action.ForciblyTerminateProcessesAction
import dev.turingcomplete.intellijjpsplugin.ui.action.GracefullyTerminateProcessesAction
import dev.turingcomplete.intellijjpsplugin.ui.action.RefreshProcessInformationAction
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjpsplugin.ui.common.overrideLeftInset
import dev.turingcomplete.intellijjpsplugin.ui.common.overrideTopInset
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent

class ProcessDescriptionPanel(val project: Project) : JBPanel<ProcessDescriptionPanel>(GridBagLayout()) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val processDescriptionLabel = JBLabel()
  private var warningLabelShown = false
  private val warningLabelBag : GridBag
  private val warningLabel = JBLabel(AllIcons.General.Warning)

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val bag = UiUtils.createDefaultGridBag()

    border = UiUtils.EMPTY_BORDER

    add(processDescriptionLabel, bag.nextLine().next().fillCellHorizontally().anchor(GridBagConstraints.WEST).weightx(1.0))
    add(createProcessToolbar(this), bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP))

    warningLabelBag = bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP / 2).coverLine()
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun processNodeUpdated(processNode: ProcessNode) {
    processDescriptionLabel.text = "<html><b>${processNode.processDescription()}</b></html>"
    processDescriptionLabel.icon = processNode.processType.icon

    // Set warning text
    val warningText = processNode.warningText
    if (warningText != null) {
      warningLabel.text = warningText
      if (!warningLabelShown) {
        add(warningLabel, warningLabelBag)
        warningLabelShown = true
      }
    }
    else if (warningLabelShown) {
      remove(warningLabel)
      warningLabelShown = false
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createProcessToolbar(parent: JComponent): JComponent {
    val processToolsGroup = DefaultActionGroup("Process Actions", true).apply {
      templatePresentation.icon = AllIcons.General.GearPlain

      add(GracefullyTerminateProcessesAction(collectJavaProcessesOnSuccess = false))
      add(ForciblyTerminateProcessesAction(collectJavaProcessesOnSuccess = false))
    }
    val topActionGroup = DefaultActionGroup(processToolsGroup, RefreshProcessInformationAction(false))

    val myToolbar = ActionManager.getInstance().createActionToolbar("${JpsToolWindowFactory.TOOLBAR_PLACE_PREFIX}.toolbar.processDetails", topActionGroup, true)
    myToolbar.setTargetComponent(parent)

    return myToolbar.component.apply {
      border = null
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}