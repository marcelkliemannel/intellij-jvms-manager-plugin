package dev.turingcomplete.intellijjpsplugin.ui.detail

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjpsplugin.process.OshiUtils
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.process.stateDescription
import dev.turingcomplete.intellijjpsplugin.ui.JavaProcessesPanel
import dev.turingcomplete.intellijjpsplugin.ui.JavaProcessesToolWindowFactory
import dev.turingcomplete.intellijjpsplugin.ui.action.ActionUtils
import dev.turingcomplete.intellijjpsplugin.ui.action.ForciblyTerminateProcessesAction
import dev.turingcomplete.intellijjpsplugin.ui.action.GracefullyTerminateProcessesAction
import dev.turingcomplete.intellijjpsplugin.ui.common.*
import org.apache.commons.io.FileUtils
import oshi.PlatformEnum
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

open class ProcessTab<T : ProcessNode>(protected val project: Project,
                                       protected val showParentProcessNodeDetails: (ProcessNode) -> Unit,
                                       private val processTerminated: () -> Unit,
                                       initialProcessNode: T,
                                       title: String = "Process") : DetailTab<T>(title, initialProcessNode) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val processDescriptionLabel = JBLabel().copyable()
  private val pidLabel = JBLabel().copyable()
  private var parentProcessWrapper: BorderLayoutPanel = BorderLayoutPanel()
  private val vszLabel = JBLabel().copyable()
  private val userLabel = JBLabel().copyable()
  private val groupLabel = JBLabel().copyable()
  private val openFilesLabel = JBLabel().copyable()
  private val readLabel = JBLabel().copyable()
  private val writtenLabel = JBLabel().copyable()
  private val stateLabel = JBLabel().copyable()
  private val priorityLabel = JBLabel().copyable()
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

      add(BorderLayoutPanel().run {
        addToLeft(processDescriptionLabel)
        addToRight(createProcessToolbar(this))
      }, bag.nextLine().next().coverLine().weightx(1.0).fillCellHorizontally())

      add(JBLabel("PID:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
      add(pidLabel, bag.next().overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally())

      add(JBLabel("Parent PID:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
      add(parentProcessWrapper, bag.next().overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally())

      addAdditionalMainInformation(bag)

      add(HyperlinkLabel("Show full path").also { hyperlinkLabel ->
        hyperlinkLabel.addHyperlinkListener { TextPopup.showAbove("Show full path of PID ${processNode.process.processID}", processNode.process.path, hyperlinkLabel) }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.LARGE_VGAP).fillCellHorizontally())

      add(HyperlinkLabel("Show command line").also { hyperlinkLabel ->
        hyperlinkLabel.addHyperlinkListener { TextPopup.showAbove("Show command line of PID ${processNode.process.processID}", processNode.process.commandLine, hyperlinkLabel, breakCommandSupported = true, breakCommand = true) }
      }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).fillCellHorizontally())

      add(HyperlinkLabel("Open working directory").apply {
        addHyperlinkListener { BrowserUtil.browse(processNode.process.currentWorkingDirectory) }
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
      add(JPanel(), bag.nextLine().next().coverLine().weightx(1.0).weighty(1.0).fillCell())

      processNodeUpdated()
    }

    override fun getData(dataId: String) : Any? = when {
      ActionUtils.SELECTED_PROCESSES.`is`(dataId) -> listOf<ProcessNode>(processNode)
      ActionUtils.SELECTED_PROCESS.`is`(dataId) -> processNode
      else -> null
    }
  }

  private fun createProcessToolbar(parent: JComponent): JComponent {
    val processToolsGroup = DefaultActionGroup("Process Actions", true).apply {
      templatePresentation.icon = AllIcons.General.GearPlain
      add(GracefullyTerminateProcessesAction().onFinished { processTerminated() })
      add(ForciblyTerminateProcessesAction().onFinished { processTerminated() })
    }
    val topActionGroup = DefaultActionGroup(processToolsGroup, createRefreshAction())

    val myToolbar = ActionManager.getInstance().createActionToolbar("${JavaProcessesToolWindowFactory.PLACE_PREFIX}.toolbar.processDetails", topActionGroup, true)
    myToolbar.setTargetComponent(parent)
    return myToolbar.component.apply {
      border = null
    }
  }

  private fun createRefreshAction() = object : DumbAwareAction(AllIcons.Actions.Refresh) {

    var performing = false

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = !performing
    }

    override fun actionPerformed(e: AnActionEvent) {
      performing = true
      UpdateProcessInformation(processNode, project, { processNodeUpdated() }, { performing = false }).queue()
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
      addHyperlinkListener {
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
    }, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("Open files:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(openFilesLabel, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).weightx(1.0).fillCellHorizontally().overrideTopInset(UIUtil.DEFAULT_VGAP))

    add(JBLabel("Read:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(readLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("Written:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(writtenLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())


    add(JBLabel("Bitness:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
    add(bitnessLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    add(JBLabel("Affinity mask:"), bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(affinityMaskLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.DEFAULT_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())


    add(JBLabel(if (PlatformEnum.WINDOWS == OshiUtils.CURRENT_PLATFORM) "Minor:" else "Minor faults:"), bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
    add(minorFailsLabel, bag.next().weightx(1.0).overrideTopInset(UIUtil.LARGE_VGAP).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).fillCellHorizontally())

    if (PlatformEnum.WINDOWS != OshiUtils.CURRENT_PLATFORM) {
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

    startTimeLabel.text = DateFormatUtil.formatDateTime(process.startTime)
    upTimeLabel.text = StringUtil.formatDuration(process.upTime)
    userTimeLabel.text = StringUtil.formatDuration(process.userTime)
    kernelTimeLabel.text = StringUtil.formatDuration(process.kernelTime)

    rssLabel.text = StringUtil.formatFileSize(process.residentSetSize)
    vszLabel.text = StringUtil.formatFileSize(process.virtualSize)

    userLabel.text = "${process.user} (${process.userID})"
    groupLabel.text = "${process.group} (${process.groupID})"

    osThreadsHyperlinkLabel.setHyperlinkText(process.threadCount.toString())
    openFilesLabel.text = process.openFiles.takeIf { it < 0 }?.toString() ?: "Unknown"
    val bytesRead = process.bytesRead
    readLabel.text = "${FileUtils.byteCountToDisplaySize(bytesRead)}${if (bytesRead == 0L) " / Unknown" else ""}"
    val bytesWritten = process.bytesWritten
    writtenLabel.text = "${FileUtils.byteCountToDisplaySize(bytesWritten)}${if (bytesWritten == 0L) " / Unknown" else ""}"

    bitnessLabel.text = process.bitness.takeIf { it > 0 }?.let { "$it Bit" } ?: "Unknown"
    affinityMaskLabel.text = process.affinityMask.takeIf { it > 0 }?.toString() ?: "Unknown"

    minorFailsLabel.text = process.minorFaults.toString()
    if (PlatformEnum.WINDOWS != OshiUtils.CURRENT_PLATFORM) {
      majorFailsLabel.text = process.majorFaults.toString() // Included in minor faults
      contextSwitchesLabel.text = process.contextSwitches.toString()
    }

    collectedAtLabel.text = "Collected at: ${DateFormatUtil.formatDate(processNode.collectedAtMillis)} ${DateFormatUtil.formatTimeWithSeconds(processNode.collectedAtMillis)}"
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

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
      HyperlinkLabel(hyperlinkText).apply { addHyperlinkListener { showParentProcessNodeDetails(processNode) } }
    }
    else {
      JBLabel("Unknown")
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class UpdateProcessInformation(val processNode: ProcessNode, project: Project,
                                         val onSuccess: () -> Unit,
                                         val onFinished: () -> Unit)
    : Task.ConditionalModal(project, "Updating process information", false, DEAF) {

    companion object {
      private val LOG = Logger.getInstance(JavaProcessesPanel::class.java)
    }

    override fun run(indicator: ProgressIndicator) {
      processNode.update()
    }

    override fun onThrowable(error: Throwable) {
      val errorMessage = "Failed to update information of PID ${processNode.process.processID}"
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(project, "$errorMessage\nSee idea.log for more details.", "Updating Process Information Failed")
    }

    override fun onSuccess() {
      onSuccess.invoke()
    }

    override fun onFinished() {
      onFinished.invoke()
    }
  }
}