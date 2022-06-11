package dev.turingcomplete.intellijjpsplugin.ui.detail

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.util.text.StringUtil.formatDuration
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.ScrollPaneFactory.createScrollPane
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.text.DateFormatUtil.formatDateTime
import com.intellij.util.ui.JBUI.emptyInsets
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjpsplugin.process.OshiUtils
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.process.action.ProcessNodeActionUtils.SELECTED_PROCESS
import dev.turingcomplete.intellijjpsplugin.process.action.ProcessNodeActionUtils.SELECTED_PROCESSES
import dev.turingcomplete.intellijjpsplugin.process.stateDescription
import dev.turingcomplete.intellijjpsplugin.ui.common.*
import org.apache.commons.io.FileUtils
import oshi.PlatformEnum
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

open class ProcessDetailPanel<T : ProcessNode>(private var processNode: T, showParentProcessDetails: (ProcessNode) -> Unit)
  : BorderLayoutPanel(), DataProvider {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val tabs = JBTabbedPane()
  private val generalPanel = GeneralPanel(processNode, showParentProcessDetails)

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    showProcessNode(processNode)

    addToCenter(tabs.apply {
      tabComponentInsets = emptyInsets()

      addTab("General", createScrollPane(generalPanel, true))
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

  fun showProcessNode(processNode: T) {
    this.processNode = processNode

    generalPanel.showProcessNode(processNode)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class GeneralPanel(var processNode: ProcessNode, val showParentProcessDetails: (ProcessNode) -> Unit) : JPanel(GridBagLayout()) {

    val processDescriptionLabel = JBLabel().copyable()
    val pidLabel = JBLabel().copyable()
    var parentProcessWrapper: BorderLayoutPanel = BorderLayoutPanel()
    val vszLabel = JBLabel().copyable()
    val userLabel = JBLabel().copyable()
    val groupLabel = JBLabel().copyable()
    val openFilesLabel = JBLabel().copyable()
    val readLabel = JBLabel().copyable()
    val writtenLabel = JBLabel().copyable()
    val stateLabel = JBLabel().copyable()
    val priorityLabel = JBLabel().copyable()
    val startTimeLabel = JBLabel().copyable()
    val upTimeLabel = JBLabel().copyable()
    val userTimeLabel = JBLabel().copyable()
    val kernelTimeLabel = JBLabel().copyable()
    val rssLabel = JBLabel().copyable()
    val osThreadsHyperlinkLabel = HyperlinkLabel()
    val bitnessLabel = JBLabel().copyable()
    val affinityMaskLabel = JBLabel().copyable()
    val minorFailsLabel = JBLabel().copyable()
    val majorFailsLabel = JBLabel().copyable()
    val contextSwitchesLabel = JBLabel().copyable()

    init {
      border = EmptyBorder(UIUtil.PANEL_REGULAR_INSETS)

      val bag = UiUtils.createDefaultGridBag()

      add(processDescriptionLabel, bag.nextLine().next().coverLine().weightx(1.0).fillCellHorizontally())

      add(JBLabel("PID:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(pidLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Parent PID:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(parentProcessWrapper, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(HyperlinkLabel("Show full path").also { hyperlinkLabel ->
        hyperlinkLabel.addHyperlinkListener { TextPopup.showAbove("Show full path of PID ${processNode.process.processID}", processNode.process.path, hyperlinkLabel) }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.LARGE_VGAP).fillCellHorizontally())

      add(HyperlinkLabel("Show command line").also { hyperlinkLabel ->
        hyperlinkLabel.addHyperlinkListener { TextPopup.showAbove("Show command line of PID ${processNode.process.processID}", processNode.process.commandLine, hyperlinkLabel, breakCommandSupported = true, breakCommand = true) }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())

      add(HyperlinkLabel("Open working directory").apply {
        addHyperlinkListener { BrowserUtil.browse(processNode.process.currentWorkingDirectory) }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())

      add(HyperlinkLabel("Show environment variables").apply {
        addHyperlinkListener {
          val columnNames = arrayOf("Key", "Value")
          val data = processNode.process.environmentVariables.map { arrayOf(it.key, it.value) }.toTypedArray()
          TablePopup.showAbove("Environment variables of PID ${processNode.process.processID}", data, columnNames, "Environment Variable", "Environment Variables", this@apply)
        }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())


      add(JBLabel("In state:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(stateLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Priority:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(priorityLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(JBLabel("Start time:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(startTimeLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Up time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(upTimeLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("User time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(userTimeLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Kernel time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(kernelTimeLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(JBLabel("RSS mem.:").apply {
        toolTipText = "The resident set size (RSS) shows how much memory is allocated to this process and is in RAM."
      }, bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(rssLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("VSZ mem.:").apply {
        toolTipText = "The virtual memory size (VSZ) shows all memory that the process can access, including memory that is swapped out and memory that is from shared libraries."
      }, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(vszLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(JBLabel("User:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(userLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Group:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(groupLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(JBLabel("OS threads:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(osThreadsHyperlinkLabel.apply {
        addHyperlinkListener {
          val columnNames = arrayOf("ID", "Name", "State", "Priority", "Start Time", "Up Time")
          val data = processNode.process.threadDetails.map { arrayOf(it.threadId.toString(),
                                                                     it.name.takeIf { it.isNotBlank() } ?: "Unknown",
                                                                     it.state.name,
                                                                     it.priority.toString(),
                                                                     formatDateTime(it.startTime),
                                                                     formatDuration(it.upTime)) }.toTypedArray()
          TablePopup.showAbove("Operating System Threads of PID ${processNode.process.processID}", data, columnNames, "Thread Information", "Thread Information", this@apply, "\t")
        }
      }, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Open files:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(openFilesLabel, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP).weightx(1.0).fillCellHorizontally().overrideTopInset(UIUtil.DEFAULT_VGAP))

      add(JBLabel("Read:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(readLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Written:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(writtenLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(JBLabel("Bitness:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(bitnessLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      add(JBLabel("Affinity mask:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(affinityMaskLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())


      add(JBLabel(if (PlatformEnum.WINDOWS == OshiUtils.CURRENT_PLATFORM) "Minor:" else "Minor faults:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(minorFailsLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

      if (PlatformEnum.WINDOWS != OshiUtils.CURRENT_PLATFORM) {
        add(JBLabel("Major faults:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
        add(majorFailsLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())

        add(JBLabel("Ctx switches:").apply {
          toolTipText = "Context switches"
        }, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
        add(contextSwitchesLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP).fillCellHorizontally())
      }
    }

    fun showProcessNode(processNode: ProcessNode) {
      this.processNode = processNode

      val process = processNode.process

      processDescriptionLabel.text = "<html><b>${processNode.processDescription()}</b></html>"
      processDescriptionLabel.icon = processNode.processType.icon

      pidLabel.text = processNode.process.processID.toString()
      parentProcessWrapper.removeAll()
      parentProcessWrapper.addToCenter(createParentProcessComponent(processNode))

      stateLabel.text = process.state.name
      stateLabel.toolTipText = process.state.stateDescription()
      priorityLabel.text = process.priority.toString()
      priorityLabel.toolTipText = processNode.priorityDescription()

      startTimeLabel.text = formatDateTime(process.startTime)
      upTimeLabel.text = formatDuration(process.upTime)
      userTimeLabel.text = formatDuration(process.userTime)
      kernelTimeLabel.text = formatDuration(process.kernelTime)

      rssLabel.text = StringUtil.formatFileSize(process.residentSetSize)
      vszLabel.text = StringUtil.formatFileSize(process.virtualSize)

      userLabel.text = "${process.user} (${process.userID})"
      groupLabel.text = "${process.group} (${process.groupID})"

      osThreadsHyperlinkLabel.setHyperlinkText(process.threadCount.toString())
      openFilesLabel.text = process.openFiles.takeIf { it < 0 }?.toString() ?: "Unknown"
      readLabel.text = FileUtils.byteCountToDisplaySize(process.bytesRead)
      writtenLabel.text = FileUtils.byteCountToDisplaySize(process.bytesWritten)

      bitnessLabel.text = process.bitness.takeIf { it > 0 }?.let { "$it Bit" } ?: "Unknown"
      affinityMaskLabel.text = process.affinityMask.takeIf { it > 0 }?.toString() ?: "Unknown"

      minorFailsLabel.text = process.minorFaults.toString()
      if (PlatformEnum.WINDOWS != OshiUtils.CURRENT_PLATFORM) {
        majorFailsLabel.text = process.majorFaults.toString() // Included in minor faults
        contextSwitchesLabel.text = process.contextSwitches.toString()
      }
    }

    private fun createParentProcessComponent(processNode: ProcessNode): JComponent {
      val parentProcessId = processNode.process.parentProcessID

      // Oshi can't resolve macOS's `launchd` process
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
        HyperlinkLabel(hyperlinkText).apply { addHyperlinkListener { showParentProcessDetails(processNode) } }
      }
      else {
        JBLabel("Unknown")
      }
    }
  }
}