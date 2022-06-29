package dev.turingcomplete.intellijjpsplugin.ui

import com.intellij.CommonBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CollapseAllAction
import com.intellij.ide.actions.ExpandAllAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjpsplugin.JpsPluginService
import dev.turingcomplete.intellijjpsplugin.process.CollectJvmProcessNodesTask
import dev.turingcomplete.intellijjpsplugin.process.FindProcessNodeTask
import dev.turingcomplete.intellijjpsplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.CommonsDataKeys.COLLECT_JVM_PROCESS_NODES_TASK_RUNNING
import dev.turingcomplete.intellijjpsplugin.ui.CommonsDataKeys.getRequiredData
import dev.turingcomplete.intellijjpsplugin.ui.action.FindProcessAction
import dev.turingcomplete.intellijjpsplugin.ui.detail.ProcessNodeDetails
import dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.JvmProcessNodeDetails
import dev.turingcomplete.intellijjpsplugin.ui.list.JvmProcessesTable
import javax.swing.JComponent
import javax.swing.SwingConstants

class JvmProcessesMainPanel(private val project: Project) : SimpleToolWindowPanel(false), Disposable, DataProvider {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(JvmProcessesMainPanel::class.java)

    private val NO_PROCESS_SELECTED_COMPONENT = BorderLayoutPanel().apply {
      addToCenter(JBLabel("No process selected", UIUtil.ComponentStyle.REGULAR, UIUtil.FontColor.BRIGHTER).apply {
        horizontalAlignment = SwingConstants.CENTER
      })
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var collectJvmProcessNodesTaskRunning = false
  private var contentSplitter = JBSplitter(0.72f)
  private val processesTable = JvmProcessesTable(project)
  private var processNodeDetails: ProcessNodeDetails<ProcessNode>? = null
  private var jvmProcessNodeDetails: JvmProcessNodeDetails? = null
  private var activeProcessDetails: ProcessNodeDetails<*>? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    toolbar = createToolbar()

    setContent(contentSplitter.apply {
      firstComponent = ScrollPaneFactory.createScrollPane(processesTable, true)
      secondComponent = NO_PROCESS_SELECTED_COMPONENT
    })
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun dispose() {
  }

  override fun getData(dataId: String): Any? = when {
    COLLECT_JVM_PROCESS_NODES_TASK_RUNNING.`is`(dataId) -> collectJvmProcessNodesTaskRunning
    else -> activeProcessDetails?.getData(dataId) ?: kotlin.run {
      processesTable.getData(dataId) ?: kotlin.run {
        super.getData(dataId)
      }
    }
  }

  fun processDetailsUpdated(processNodes: List<ProcessNode>) {
    processesTable.processDetailsUpdated(processNodes)
    activeProcessDetails?.processDetailsUpdated()
  }

  fun showProcessDetails(processNode: ProcessNode) {
    if (processNode is JvmProcessNode) {
      jvmProcessNodeDetails?.let { it.processNode = processNode } ?: run {
        jvmProcessNodeDetails = JvmProcessNodeDetails(project, showParentProcessNodeDetails(), processNode)
      }
      contentSplitter.secondComponent = jvmProcessNodeDetails!!.component
      activeProcessDetails = jvmProcessNodeDetails
    }
    else {
      processNodeDetails?.let { it.processNode = processNode } ?: run {
        processNodeDetails = ProcessNodeDetails(project, showParentProcessNodeDetails(), processNode)
      }
      contentSplitter.secondComponent = processNodeDetails!!.component
      activeProcessDetails = processNodeDetails
    }
  }

  fun collectJvmProcesses() {
    if (collectJvmProcessNodesTaskRunning) {
      return
    }

    val onSuccess: (List<JvmProcessNode>) -> Unit = { jvmProcessNodes ->
      processesTable.setJvmProcessNodes(jvmProcessNodes)
    }

    val onFinished: () -> Unit = {
      collectJvmProcessNodesTaskRunning = false
      processesTable.isEnabled = true
      processNodeDetails?.setEnabled(true)
      jvmProcessNodeDetails?.setEnabled(true)
    }

    val onThrowable: (Throwable) -> Unit = { error ->
      val errorMessage = "Failed to collect JVM processes: ${error.message}"
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(project, "$errorMessage\n\nSee idea.log for more details.", "Collecting JVM Processes Failed")
    }

    collectJvmProcessNodesTaskRunning = true
    processesTable.isEnabled = false
    processNodeDetails?.setEnabled(false)
    jvmProcessNodeDetails?.setEnabled(false)
    CollectJvmProcessNodesTask(project, onSuccess, onFinished, onThrowable).queue()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createToolbar(): JComponent {
    val toolbarGroup = DefaultActionGroup().apply {
      add(ReloadJvmProcessesAction())
      addSeparator()
      add(ExpandAllAction { processesTable.treeExpander })
      add(CollapseAllAction { processesTable.treeExpander })
      addSeparator()
      add(FindProcessAction())
      addSeparator()
      add(ShowJpsSettings())
    }

    return ActionManager.getInstance()
            .createActionToolbar("${JpsToolWindowFactory.TOOLBAR_PLACE_PREFIX}.toolbar.processes", toolbarGroup, false)
            .run {
              setTargetComponent(this@JvmProcessesMainPanel)
              component
            }
  }

  private fun showParentProcessNodeDetails(): (ProcessNode) -> Unit = { processNode ->
    val parentProcessNode = processNode.parent
    if (parentProcessNode is ProcessNode) {
      showProcessDetails(parentProcessNode)
    }
    else {
      collectParentProcessNodeDetails(processNode)
    }
  }

  private fun collectParentProcessNodeDetails(processNode: ProcessNode) {
    processNodeDetails?.setEnabled(false)
    jvmProcessNodeDetails?.setEnabled(false)

    val parentProcessId = processNode.process.parentProcessID

    val onSuccess: (ProcessNode?) -> Unit = { parentProcessNode ->
      if (parentProcessNode != null) {
        showProcessDetails(parentProcessNode)
        processNode.setParent(parentProcessNode)
      }
      else {
        Messages.showInfoMessage(project, "Unable to find information about process with PID $parentProcessId.", "Collecting Process Information")
      }
    }

    val onFinished: () -> Unit = {
      processNodeDetails?.setEnabled(true)
      jvmProcessNodeDetails?.setEnabled(true)
    }

    val onThrowable: (Throwable) -> Unit = { error ->
      val errorMessage = "Failed to collect information about process with PID $parentProcessId: ${error.message}"
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(project, "$errorMessage\n\nSee idea.log for more details.", "Collecting Process Information Failed")
    }

    FindProcessNodeTask(parentProcessId, project, onSuccess, onFinished, onThrowable).queue()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class ReloadJvmProcessesAction : DumbAwareAction("Reload", null, AllIcons.Actions.Refresh) {

    override fun update(e: AnActionEvent) {
      e.presentation.isEnabled = !getRequiredData(COLLECT_JVM_PROCESS_NODES_TASK_RUNNING, e.dataContext)
    }

    override fun actionPerformed(e: AnActionEvent) {
      e.project?.getService(JpsPluginService::class.java)?.collectJavaProcesses()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class ShowJpsSettings : DumbAwareAction("Open JPS ${CommonBundle.settingsTitle()}", null, AllIcons.General.GearPlain) {

    override fun actionPerformed(e: AnActionEvent) {
      e.project?.getService(JpsPluginService::class.java)?.showSettings()
    }
  }
}