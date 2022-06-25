package dev.turingcomplete.intellijjpsplugin.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CollapseAllAction
import com.intellij.ide.actions.ExpandAllAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
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
import dev.turingcomplete.intellijjpsplugin.process.CollectJavaProcessNodesTask
import dev.turingcomplete.intellijjpsplugin.process.FindProcessNodeTask
import dev.turingcomplete.intellijjpsplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.common.IntInputValidator
import dev.turingcomplete.intellijjpsplugin.ui.detail.ProcessNodeDetails
import dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.JvmProcessNodeDetails
import dev.turingcomplete.intellijjpsplugin.ui.list.JavaProcessesTable
import javax.swing.JComponent
import javax.swing.SwingConstants

class JavaProcessesPanel(private val project: Project) : SimpleToolWindowPanel(false), Disposable {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(JavaProcessesPanel::class.java)

    private val NO_PROCESS_SELECTED_COMPONENT = BorderLayoutPanel().apply {
      addToCenter(JBLabel("No process selected", UIUtil.ComponentStyle.REGULAR, UIUtil.FontColor.BRIGHTER).apply {
        horizontalAlignment = SwingConstants.CENTER
      })
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val javaProcessesCollectionTask = createJavaProcessesCollectionTask()

  private var contentSplitter = JBSplitter(0.72f)
  private val processesTable: JavaProcessesTable
  private var processNodeDetails: ProcessNodeDetails<ProcessNode>? = null
  private var jvmProcessNodeDetails: JvmProcessNodeDetails? = null

  private var collectJavaProcessesInProgress = false

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    toolbar = createToolbar()

    processesTable = JavaProcessesTable({ collectJavaProcesses() }, { showProcessDetails(it) })

    setContent(contentSplitter.apply {
      firstComponent = ScrollPaneFactory.createScrollPane(processesTable, true)
      secondComponent = NO_PROCESS_SELECTED_COMPONENT
    })
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun dispose() {
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createToolbar(): JComponent {
    val toolbarGroup = DefaultActionGroup().apply {
      add(createReloadAction())
      addSeparator()
      add(ExpandAllAction { processesTable.treeExpander })
      add(CollapseAllAction { processesTable.treeExpander })
      addSeparator()
      add(createFindProcessNodeAction())
    }

    return ActionManager.getInstance()
            .createActionToolbar("${JavaProcessesToolWindowFactory.PLACE_PREFIX}.toolbar.processes", toolbarGroup, false)
            .run {
              setTargetComponent(this@JavaProcessesPanel)
              component
            }
  }

  private fun createReloadAction(): AnAction {
    return object : DumbAwareAction("Reload", null, AllIcons.Actions.Refresh) {

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !collectJavaProcessesInProgress
      }

      override fun actionPerformed(e: AnActionEvent) {
        collectJavaProcesses()
      }
    }
  }

  private fun createFindProcessNodeAction(): AnAction {
    return object : DumbAwareAction("Find Process Information", null, AllIcons.Actions.Search) {

      private var findInProcess = false

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !findInProcess
      }

      override fun actionPerformed(e: AnActionEvent) {
        val project = CommonDataKeys.PROJECT.getData(e.dataContext)

        val pid = Messages.showInputDialog(project, "Please enter a PID:", "Find Process Information",
                                              null, null, IntInputValidator.INSTANCE)?.toIntOrNull() ?: return

        val onSuccess: (ProcessNode?) -> Unit = {
          if (it != null) {
            showProcessDetails(it)
          }
          else {
            Messages.showErrorDialog(project, "No process with PID $pid found.", "Finding Process Information Failed")
          }
        }

        val onThrowable: (Throwable) -> Unit = { error ->
          val errorMessage = "Failed to find process: ${error.message}"
          LOG.warn(errorMessage, error)
          Messages.showErrorDialog(project, "$errorMessage\n\nSee idea.log for more details.", "Finding Process Information Failed")
        }

        findInProcess= true
        FindProcessNodeTask(pid, project, onSuccess, { findInProcess = false }, onThrowable).queue()
      }
    }
  }

  private fun collectJavaProcesses() {
    if (collectJavaProcessesInProgress) {
      return
    }

    collectJavaProcessesInProgress = true
    processesTable.isEnabled = false
    processNodeDetails?.setEnabled(false)
    jvmProcessNodeDetails?.setEnabled(false)

    javaProcessesCollectionTask.queue()
  }

  private fun createJavaProcessesCollectionTask(): CollectJavaProcessNodesTask {
    val onSuccess: (List<JvmProcessNode>) -> Unit = { javaProcessNodes ->
      processesTable.setJavaProcessNodes(javaProcessNodes)
    }

    val onFinished: () -> Unit = {
      collectJavaProcessesInProgress = false
      processesTable.isEnabled = true
      processNodeDetails?.setEnabled(true)
      jvmProcessNodeDetails?.setEnabled(true)
    }

    val onThrowable: (Throwable) -> Unit = { error ->
      val errorMessage = "Failed to collect information about all Java processes: ${error.message}"
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(project, "$errorMessage\n\nSee idea.log for more details.", "Collecting Java Processes Information Failed")
    }

    return CollectJavaProcessNodesTask(project, onSuccess, onFinished, onThrowable)
  }

  private fun showProcessDetails(processNode: ProcessNode) = when (processNode) {
    is JvmProcessNode -> showJvmProcessNode(processNode)
    else -> showProcessNode(processNode)
  }

  private fun showJvmProcessNode(processNode: JvmProcessNode) {
    jvmProcessNodeDetails?.let { it.processNode = processNode } ?: run {
      jvmProcessNodeDetails = JvmProcessNodeDetails(project, showParentProcessNodeDetails(), processTerminated(), processNode)
    }
    contentSplitter.secondComponent = jvmProcessNodeDetails!!.component
  }

  private fun showProcessNode(processNode: ProcessNode) {
    processNodeDetails?.let { it.processNode = processNode } ?: run {
      processNodeDetails = ProcessNodeDetails(project, showParentProcessNodeDetails(), processTerminated(), processNode)
    }
    contentSplitter.secondComponent = processNodeDetails!!.component
  }

  private fun processTerminated(): () -> Unit = {
    collectJavaProcesses()
    contentSplitter.secondComponent = NO_PROCESS_SELECTED_COMPONENT
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
}