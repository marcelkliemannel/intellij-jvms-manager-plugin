package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjvmsmanagerplugin.process.OshiUtils
import dev.turingcomplete.intellijjvmsmanagerplugin.process.OshiUtils.isPlatform
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.process.stateDescription
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.CommonsDataKeys
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.*
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.RunCommandTask
import org.apache.commons.io.FileUtils
import oshi.PlatformEnum.*
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import javax.swing.event.HyperlinkEvent

open class ProcessTab<T : ProcessNode>(protected val project: Project,
                                       protected val showParentProcessNodeDetails: (ProcessNode) -> Unit,
                                       initialProcessNode: T,
                                       title: String = "Process") : DetailTab<T>(title, initialProcessNode) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val processDescriptionPanel = ProcessDescriptionPanel(project)

  private val pidLabel = JBLabel().copyable()
  private var parentProcessWrapper: BorderLayoutPanel = BorderLayoutPanel()
  private val vszLabel = JBLabel().copyable()
  private val userLabel = JBLabel() // Not copyable because of tooltip
  private val groupLabel = JBLabel() // Not copyable because of tooltip
  private val readLabel = JBLabel().copyable()
  private val writtenLabel = JBLabel().copyable()
  private val openFilesLabel = JBLabel().copyable()
  private val listOpenFilesHyperlinkLabel = HyperlinkLabel()
  private val listOpenPortsHyperlinkLabel = HyperlinkLabel()
  private val stateLabel = JBLabel().copyable()
  private val priorityLabel = JBLabel() // Not copyable because of tooltip
  private val startTimeLabel = JBLabel().copyable()
  private val upTimeLabel = JBLabel().copyable()
  private val userTimeLabel = JBLabel().copyable()
  private val kernelTimeLabel = JBLabel().copyable()
  private val rssLabel = JBLabel().copyable()
  private val osThreadsHyperlinkLabel = HyperlinkLabel()
  private val bitnessLabel = JBLabel().copyable()
  private val affinityMaskLabel = JBLabel().copyable()
  private val minorFailsLabel = JBLabel().copyable()
  private val majorFailsLabel = JBLabel().copyable()
  private val contextSwitchesLabel = JBLabel().copyable()
  private val collectedAtLabel = JBLabel("", UIUtil.ComponentStyle.SMALL, UIUtil.FontColor.BRIGHTER)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  final override fun createComponent() : JComponent = object : JPanel(GridBagLayout()), DataProvider {
    init {
      border = EmptyBorder(UIUtil.PANEL_REGULAR_INSETS)

      val bag = UiUtils.createDefaultGridBag()

      add(processDescriptionPanel, bag.nextLine().next().coverLine().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

      add(JBLabel("PID:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP))
      add(pidLabel, bag.next().overrideTopInset(UIUtil.DEFAULT_HGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally())

      add(JBLabel("Parent PID:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(parentProcessWrapper, bag.next().overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally())

      addAdditionalMainInformation(bag)

      add(HyperlinkLabel("Show full path").also { hyperlinkLabel ->
        hyperlinkLabel.addHyperlinkListener { TextPopup.showAbove("Show full path of PID ${processNode.process.processID}", processNode.process.path ?: "", hyperlinkLabel) }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.LARGE_VGAP).fillCellHorizontally())

      add(HyperlinkLabel("Show command line").also { hyperlinkLabel ->
        hyperlinkLabel.addHyperlinkListener { TextPopup.showAbove("Show command line of PID ${processNode.process.processID}", processNode.process.commandLine ?: "", hyperlinkLabel, breakCommandSupported = true, breakCommand = true) }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())

      add(HyperlinkLabel("Open working directory").apply {
        addHyperlinkListener(createOpenWorkingDirectoryListener())
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())

      add(HyperlinkLabel("Show environment variables").also { hyperLinkLabel ->
        hyperLinkLabel.addHyperlinkListener {
          val columnNames = arrayOf("Key", "Value")
          val data = processNode.process.environmentVariables.map { arrayOf(it.key, it.value) }.sortedBy { it[0] }.toTypedArray()
          TablePopup.showAbove("Environment variables of PID ${processNode.process.processID}", data, columnNames, "Environment Variable", "Environment Variables", hyperLinkLabel)
        }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())

      add(createDetailsComponent(), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP).coverLine().weightx(1.0).fillCellHorizontally())

      add(collectedAtLabel, bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP).coverLine().weightx(1.0).fillCellHorizontally())

      // Fill rest of panel
      add(UiUtils.EMPTY_FILL_PANEL(), bag.nextLine().next().coverLine().weightx(1.0).weighty(1.0).fillCell())

      processNodeUpdated()
    }

    override fun getData(dataId: String) : Any? = when {
      CommonsDataKeys.SELECTED_PROCESSES_DATA_KEY.`is`(dataId) -> listOf<ProcessNode>(processNode)
      else -> null
    }
  }

  private fun createOpenWorkingDirectoryListener(): (e: HyperlinkEvent) -> Unit = {
    val currentWorkingDirectory = processNode.process.currentWorkingDirectory
    if (currentWorkingDirectory == null) {
      Messages.showErrorDialog(project,
                               "The working directory of this process is unknown.",
                               "Open Working Directory Failed")
    }
    else {
      BrowserUtil.browse(currentWorkingDirectory)
    }
  }

  private fun createDetailsComponent(): JPanel = JPanel(GridBagLayout()).apply {
    val bag = UiUtils.createDefaultGridBag()

    add(JBLabel("In state:"), bag.nextLine().next())
    add(stateLabel, bag.next().weightx(1.0).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("Priority:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(priorityLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())


    add(JBLabel("Start time:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
    add(startTimeLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("Up time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(upTimeLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("User time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(userTimeLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("Kernel time:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(kernelTimeLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())


    add(JBLabel("RSS memory:").apply {
      toolTipText = "The resident set size (RSS) shows how much memory is allocated to this process and is in RAM."
    }, bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
    add(rssLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("VSZ memory:").apply {
      toolTipText = "The virtual memory size (VSZ) shows all memory that the process can access, including memory that is swapped out and memory that is from shared libraries."
    }, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(vszLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())


    add(JBLabel("User:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
    add(userLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("Group:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(groupLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())


    add(JBLabel("OS threads:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
    add(osThreadsHyperlinkLabel.apply {
      addHyperlinkListener(createOsThreadsHyperlinkListener())
    }, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("Read:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(readLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("Written:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(writtenLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("Open files:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    val openFilesComponent = if (isPlatform(LINUX, MACOS)) {
      listOpenFilesHyperlinkLabel.apply { addHyperlinkListener(createListOpenFilesHyperlinkListener()) }
    }
    else {
      openFilesLabel
    }
    add(openFilesComponent, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    if (isPlatform(LINUX, MACOS, WINDOWS)) {
      add(listOpenPortsHyperlinkLabel.apply {
        setHyperlinkText("List open ports")
        addHyperlinkListener(createListOpenPortsHyperlinkListener())
      }, bag.nextLine().next().weightx(1.0).coverLine(2).fillCellHorizontally().overrideTopInset(UIUtil.LARGE_VGAP))
    }

    add(JBLabel("Bitness:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
    add(bitnessLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("Affinity mask:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(affinityMaskLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())


    add(JBLabel(if (WINDOWS == OshiUtils.CURRENT_PLATFORM) "Faults:" else "Minor faults:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
    add(minorFailsLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    if (WINDOWS != OshiUtils.CURRENT_PLATFORM) {
      add(JBLabel("Major faults:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(majorFailsLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

      add(JBLabel("Ctx. switches:").apply {
        toolTipText = "Context switches"
      }, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(contextSwitchesLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())
    }
  }

  protected open fun JPanel.addAdditionalMainInformation(bag: GridBag) {
  }

  override fun processNodeUpdated() {
    processDescriptionPanel.processNodeUpdated(processNode)

    val process = processNode.process

    pidLabel.text = processNode.process.processID.toString()
    parentProcessWrapper.removeAll()
    parentProcessWrapper.addToCenter(createParentProcessComponent(processNode))

    stateLabel.text = process.state.name
    stateLabel.toolTipText = process.state.stateDescription()
    priorityLabel.text = process.priority.toString()
    priorityLabel.toolTipText = processNode.priorityDescription()

    startTimeLabel.text = DateFormatUtil.formatDateTime(process.startTime)
    upTimeLabel.text = StringUtil.formatDuration(process.upTime)
    userTimeLabel.text = StringUtil.formatDuration(process.userTime)
    kernelTimeLabel.text = StringUtil.formatDuration(process.kernelTime)

    rssLabel.text = StringUtil.formatFileSize(process.residentSetSize)
    vszLabel.text = StringUtil.formatFileSize(process.virtualSize)

    userLabel.text = process.user?.toString() ?: "Unknown"
    userLabel.toolTipText = "ID: ${process.userID}"
    groupLabel.text = process.group?.toString() ?: "Unknown"
    groupLabel.toolTipText = "ID: ${process.groupID}"

    osThreadsHyperlinkLabel.setHyperlinkText(process.threadCount.toString())
    val bytesRead = process.bytesRead
    readLabel.text = "${FileUtils.byteCountToDisplaySize(bytesRead)}${if (bytesRead == 0L) " / Unknown" else ""}"
    val bytesWritten = process.bytesWritten
    writtenLabel.text = "${FileUtils.byteCountToDisplaySize(bytesWritten)}${if (bytesWritten == 0L) " / Unknown" else ""}"
    openFilesLabel.text = process.openFiles.takeIf { it < 0 }?.toString() ?: "Unknown"
    listOpenFilesHyperlinkLabel.setHyperlinkText(process.openFiles.takeIf { it < 0 }?.toString() ?: "List")

    bitnessLabel.text = process.bitness.takeIf { it > 0 }?.let { "$it Bit" } ?: "Unknown"
    affinityMaskLabel.text = process.affinityMask.takeIf { it > 0 }?.toString() ?: "Unknown"

    minorFailsLabel.text = process.minorFaults.toString()
    if (WINDOWS != OshiUtils.CURRENT_PLATFORM) {
      majorFailsLabel.text = process.majorFaults.toString() // Included in minor faults
      contextSwitchesLabel.text = process.contextSwitches.toString()
    }

    collectedAtLabel.text = "Collected at: ${DateFormatUtil.formatDate(processNode.collectedAtMillis)} ${DateFormatUtil.formatTimeWithSeconds(processNode.collectedAtMillis)}"
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createParentProcessComponent(processNode: ProcessNode): JComponent {
    val parentProcessId = processNode.process.parentProcessID

    // Oshi can't resolve macOS's `launchd` process
    if (parentProcessId == 1 && MACOS == OshiUtils.CURRENT_PLATFORM) {
      return JBLabel("1 | launchd")
    }

    val parentProcessNode = processNode.parent
    val hyperlinkText: String? = when {
      parentProcessNode is ProcessNode -> "$parentProcessId | ${parentProcessNode.processDescription()}"
      parentProcessId > 0 -> parentProcessId.toString()
      else -> null
    }

    return if (hyperlinkText != null) {
      HyperlinkLabel(hyperlinkText).apply { addHyperlinkListener { showParentProcessNodeDetails(processNode) } }
    }
    else {
      JBLabel("Unknown")
    }
  }

  private fun createOsThreadsHyperlinkListener(): (e: HyperlinkEvent) -> Unit = {
    val columnNames = arrayOf("ID", "Name", "State", "Priority", "Start Time", "Up Time")
    val data = processNode.process.threadDetails.map {
      arrayOf(it.threadId.toString(),
              it.name.ifBlank { "Unknown" },
              it.state.name,
              it.priority.toString(),
              DateFormatUtil.formatDateTime(it.startTime),
              StringUtil.formatDuration(it.upTime))
    }.toTypedArray()
    TablePopup.showAbove("Operating System Threads of PID ${processNode.process.processID}", data, columnNames, "Thread Information", "Thread Information", osThreadsHyperlinkLabel, "\t")
  }

  private fun createListOpenFilesHyperlinkListener(): (e: HyperlinkEvent) -> Unit = {
    assert(isPlatform(LINUX, MACOS))

    val pid = processNode.process.processID.toString()
    val commandLine = GeneralCommandLine("lsof", "-p", pid)
    RunCommandTask(project, "Collecting open files", "Failed to collect open files of PID $pid", commandLine, { output ->
      TextPopup.showCenteredInCurrentWindow("Open Files of PID $pid", output, project, false)
    }).queue()
  }

  private fun createListOpenPortsHyperlinkListener(): (e: HyperlinkEvent) -> Unit = {
    assert(isPlatform(WINDOWS, LINUX, MACOS))

    val pid = processNode.process.processID.toString()
    val commandLine = when {
      isPlatform(WINDOWS) -> GeneralCommandLine("powershell", "-inputformat", "none", "-outputformat", "text", "-NonInteractive", "-Command", "get-nettcpconnection | where {(\$_.OwningProcess -eq ${pid})}")
      isPlatform(LINUX, MACOS) -> GeneralCommandLine("lsof", "-Pan", "-p", pid, "-i")
      else -> throw IllegalStateException("snh: Platform not supported")
    }
    RunCommandTask(project, "Collecting open ports", "Failed to collect open ports of PID $pid", commandLine, { output ->
      TextPopup.showCenteredInCurrentWindow("Open Ports of PID $pid", output, project, false)
    }).queue()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}