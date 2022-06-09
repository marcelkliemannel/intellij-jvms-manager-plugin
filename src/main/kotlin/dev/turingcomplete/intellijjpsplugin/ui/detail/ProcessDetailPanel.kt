package dev.turingcomplete.intellijjpsplugin.ui.detail

import com.intellij.ide.BrowserUtil
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
import dev.turingcomplete.intellijjpsplugin.process.OshiUtils
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.process.action.ProcessNodeActionUtils.SELECTED_PROCESS
import dev.turingcomplete.intellijjpsplugin.process.action.ProcessNodeActionUtils.SELECTED_PROCESSES
import dev.turingcomplete.intellijjpsplugin.ui.common.*
import org.apache.commons.io.FileUtils
import oshi.PlatformEnum
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

      val process = processNode.process

      val bag = UiUtils.createDefaultGridBag()

      add(JBLabel("<html><b>${processNode.processDescription()}</b></html>", processNode.processType.icon, SwingConstants.LEFT).copyable(), bag.nextLine().next().coverLine().weightx(1.0).fillCellHorizontally())


      add(JBLabel("PID:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(JBLabel(process.processID.toString()), bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Parent PID:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(createParentProcessComponent(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(HyperlinkLabel("Show full path").also { hyperlinkLabel ->
        hyperlinkLabel.addHyperlinkListener { TextPopup.showAbove("Show full path", process.path, hyperlinkLabel) }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.LARGE_VGAP).fillCellHorizontally())

      add(HyperlinkLabel("Show command line").also { hyperlinkLabel ->
        hyperlinkLabel.addHyperlinkListener { TextPopup.showAbove("Show command line", process.commandLine, hyperlinkLabel, breakCommandSupported = true, breakCommand = true) }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())

      add(HyperlinkLabel("Open working directory").apply {
        addHyperlinkListener { BrowserUtil.browse(process.currentWorkingDirectory) }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())


      add(JBLabel("In state:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(JBLabel(process.state.name).apply { toolTipText = processNode.stateDescription() }, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Priority:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(process.priority.toString().apply { toolTipText = processNode.priorityDescription() }), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(JBLabel("Start time:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(JBLabel(DateFormatUtil.formatDateTime(process.startTime)).copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Up time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(StringUtil.formatDuration(process.upTime)).copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("User time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(StringUtil.formatDuration(process.userTime)).copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Kernel time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(StringUtil.formatDuration(process.kernelTime)).copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(JBLabel("User:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(JBLabel("${process.user} (${process.userID})").copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Group:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel("${process.group} (${process.groupID})").copyable(), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Open files:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      val openFiles = process.openFiles.takeIf { it < 0 }?.toString() ?: "Unknown"
      add(JBLabel(openFiles), bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP).weightx(1.0).fillCellHorizontally().overrideTopInset(UIUtil.DEFAULT_VGAP))

      add(JBLabel("Read:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(FileUtils.byteCountToDisplaySize(process.bytesRead)), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Written:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(JBLabel(FileUtils.byteCountToDisplaySize(process.bytesWritten)), bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      // Fill rest of panel
      add(JPanel(), bag.nextLine().next().fillCell())
    }
  }

  private fun createParentProcessComponent(): JComponent {
    val parentProcessId = processNode.process.parentProcessID

    // Edge cases

    // Oshi can't "find" the launchd process
    if (parentProcessId == 1 && PlatformEnum.MACOS == OshiUtils.CURRENT_PLATFORM) {
      return JBLabel("1 | launchd")
    }

    val parentProcessNode = processNode.parent
    val hyperlinkText: String? = when {
      parentProcessNode is ProcessNode -> "$parentProcessId | ${parentProcessNode.processDescription()}"
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