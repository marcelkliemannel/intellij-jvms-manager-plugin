package dev.turingcomplete.intellijjpsplugin.ui.detail

import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory.createScrollPane
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBUI.emptyInsets
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.process.action.ProcessNodeActionUtils.SELECTED_PROCESS
import dev.turingcomplete.intellijjpsplugin.process.action.ProcessNodeActionUtils.SELECTED_PROCESSES
import dev.turingcomplete.intellijjpsplugin.ui.UiUtils
import dev.turingcomplete.intellijjpsplugin.ui.copyable
import dev.turingcomplete.intellijjpsplugin.ui.overrideLeftInset
import dev.turingcomplete.intellijjpsplugin.ui.overrideTopInset
import org.apache.commons.io.FileUtils
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.border.EmptyBorder

class ProcessDetailPanel(private val processNode: ProcessNode, val showParentProcessDetails: () -> Unit)
  : BorderLayoutPanel(), DataProvider {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    val NO_PROCESS_SELECTED = BorderLayoutPanel().apply {
      background = JBColor.background()
      foreground = UIUtil.getLabelDisabledForeground()

      addToCenter(JBLabel("No process selected", SwingConstants.CENTER))
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val tabs = JBTabbedPane()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    addToCenter(tabs.apply {
      tabComponentInsets = emptyInsets()

      addTab("General", createScrollPane(createGeneralPanel(), true))
    })
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun getData(dataId: String): Any? {
    return when {
      SELECTED_PROCESSES.`is`(dataId) -> listOf(processNode)
      SELECTED_PROCESS.`is`(dataId) -> processNode
      else -> null
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createGeneralPanel(): JComponent {
    return JPanel(GridBagLayout()).apply {
      border = EmptyBorder(UIUtil.PANEL_REGULAR_INSETS)

      val bag = UiUtils.createDefaultGridBag()

      add(JBLabel("<html><b>${processNode.processDescription()}</b></html>", processNode.processType.icon, SwingConstants.LEFT).copyable(), bag.nextLine().next().coverLine().weightx(1.0).fillCellHorizontally())


      add(JBLabel("PID:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(JBLabel(processNode.process.processID.toString()), bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Parent PID:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(createParentProcessComponent(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(HyperlinkLabel("Show full path"), bag.nextLine().next().coverLine().overrideTopInset(UIUtil.LARGE_VGAP).fillCellHorizontally())
      add(HyperlinkLabel("Show command line"), bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())
      add(HyperlinkLabel("Show arguments"), bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())
      add(HyperlinkLabel("Open working directory"), bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())


      add(JBLabel("In state:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(JBLabel(processNode.process.state.name).apply { toolTipText = processNode.stateDescription() }, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Priority:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(processNode.process.priority.toString().apply { toolTipText = processNode.priorityDescription() }), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(JBLabel("Start time:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(JBLabel(DateFormatUtil.formatDateTime(processNode.process.startTime)).copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Up time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(StringUtil.formatDuration(processNode.process.upTime)).copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("User time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(StringUtil.formatDuration(processNode.process.userTime)).copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Kernel time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(StringUtil.formatDuration(processNode.process.kernelTime)).copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(JBLabel("User:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(JBLabel("${processNode.process.user} (${processNode.process.userID})").copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Group:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel("${processNode.process.group} (${processNode.process.groupID})").copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Open files:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      val openFiles = processNode.process.openFiles.takeIf { it < 0 }?.toString() ?: "Unknown"
      add(JBLabel(openFiles), bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP).weightx(1.0).fillCellHorizontally().overrideTopInset(UIUtil.DEFAULT_VGAP))

      add(JBLabel("Read:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(FileUtils.byteCountToDisplaySize(processNode.process.bytesRead)), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Written:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(FileUtils.byteCountToDisplaySize(processNode.process.bytesWritten)), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      // Fill rest of panel
      add(JPanel(), bag.nextLine().next().fillCell())
    }
  }

  private fun createParentProcessComponent(): JComponent {
    val parentProcessId = processNode.process.parentProcessID

    val parentProcessNode = processNode.parent
    val hyperlinkText : String? = when {
      parentProcessNode is ProcessNode -> "$parentProcessId - ${parentProcessNode.processDescription()}"
      parentProcessId > 0 -> parentProcessId.toString()
      else -> null
    }

    return if (hyperlinkText != null) {
      HyperlinkLabel(hyperlinkText).apply { addHyperlinkListener { showParentProcessDetails() } }
    }
    else {
      JBLabel("Unknown")
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}