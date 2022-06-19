package dev.turingcomplete.intellijjpsplugin.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CollapseAllAction
import com.intellij.ide.actions.ExpandAllAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBColor
import com.intellij.ui.JBSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjpsplugin.process.CollectJavaProcessNodesTask
import dev.turingcomplete.intellijjpsplugin.process.CollectProcessNodeTask
import dev.turingcomplete.intellijjpsplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.JvmProcessDetails
import dev.turingcomplete.intellijjpsplugin.ui.detail.ProcessDetails
import dev.turingcomplete.intellijjpsplugin.ui.list.JavaProcessesTable
import javax.swing.JComponent
import javax.swing.SwingConstants

class JavaProcessesPanel(private val project: Project) : SimpleToolWindowPanel(false), Disposable {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(JavaProcessesPanel::class.java)
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val javaProcessesCollectionTask = createJavaProcessesCollectionTask()

  private var contentSplitter = JBSplitter(0.75f)
  private val processesTable: JavaProcessesTable

  // The `ProcessDetails` are reused to keep the memory footprint low.
  private var processDetails: ProcessDetails<ProcessNode>? = null
  private var jvmProcessDetails: JvmProcessDetails? = null

  private var collectJavaProcessesInProgress = false

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    toolbar = createToolbar()

    processesTable = JavaProcessesTable({ collectJavaProcesses() }, { showProcessDetails(it) })

    setContent(contentSplitter.apply {
      firstComponent = ScrollPaneFactory.createScrollPane(processesTable, true)
      secondComponent = createNoProcessSelectedComponent()
    })
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun dispose() {
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createToolbar(): JComponent {
    val toolbarGroup = DefaultActionGroup(createReloadAction(),
                                          ExpandAllAction { processesTable.treeExpander },
                                          CollapseAllAction { processesTable.treeExpander })

    return ActionManager.getInstance()
            .createActionToolbar("${JavaProcessesToolWindowFactory.PLACE_PREFIX}.toolbar.processes", toolbarGroup, false)
            .run {
              setTargetComponent(this@JavaProcessesPanel)
              component
            }
  }

  private fun createNoProcessSelectedComponent(): JComponent {
    return BorderLayoutPanel().apply {
      background = JBColor.background()
      foreground = UIUtil.getLabelBackground()

      addToCenter(JBLabel("No process selected", SwingConstants.CENTER))
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

  private fun collectJavaProcesses() {
    if (collectJavaProcessesInProgress) {
      return
    }

    collectJavaProcessesInProgress = true
    processesTable.isEnabled = false

    javaProcessesCollectionTask.queue()
  }

  private fun createJavaProcessesCollectionTask(): CollectJavaProcessNodesTask {
    val onSuccess: (List<JvmProcessNode>) -> Unit = { javaProcessNodes ->
      processesTable.setJavaProcessNodes(javaProcessNodes)
    }

    val onFinished :  () -> Unit = {
      collectJavaProcessesInProgress = false
      processesTable.isEnabled = true
      processDetails?.setEnabled(true)
      jvmProcessDetails?.setEnabled(true)
    }

    val onThrowable: (Throwable) -> Unit = { error ->
      val errorMessage = "Failed to collect information about all Java processes: ${error.message}"
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(project,
                               "$errorMessage\n" +
                               "See idea.log for more details.\nIf you think this error should not appear, please report a bug.",
                               "Collecting Java Processes Information Failed")
    }

    return CollectJavaProcessNodesTask(project, onSuccess, onFinished, onThrowable)
  }

  private fun showProcessDetails(processNode: ProcessNode) {
    contentSplitter.secondComponent = when (processNode) {
      is JvmProcessNode -> updateJvmProcessDetails(processNode)
      else -> updateProcessDetails(processNode)
    }.component
  }

  /**
   * Initializes the [jvmProcessDetails] or updates the existing one via
   * [JvmProcessDetails.showProcessNode].
   */
  private fun updateJvmProcessDetails(processNode: JvmProcessNode): JvmProcessDetails {
    if (jvmProcessDetails == null) {
      jvmProcessDetails = JvmProcessDetails(project, processNode, showParentProcessNodeDetails())
    }
    else {
      jvmProcessDetails!!.showProcessNode(processNode)
    }

    return jvmProcessDetails!!
  }

  /**
   * Initializes the [processDetails] or updates the existing one via
   * [ProcessDetails.showProcessNode].
   */
  private fun updateProcessDetails(processNode: ProcessNode): ProcessDetails<ProcessNode> {
    assert(processNode::class.equals(ProcessNode::class))

    if (processDetails == null) {
      processDetails = ProcessDetails(processNode, showParentProcessNodeDetails())
    }
    else {
      processDetails!!.showProcessNode(processNode)
    }

    return processDetails!!
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
    processDetails?.setEnabled(false)
    jvmProcessDetails?.setEnabled(false)

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

    val onFinished : () -> Unit = {
      processDetails?.setEnabled(true)
      jvmProcessDetails?.setEnabled(true)
    }

    val onThrowable: (Throwable) -> Unit = { error ->
      val errorMessage = "Failed to collect information about process with PID $parentProcessId: ${error.message}"
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(project,
                               "$errorMessage\n" +
                               "See idea.log for more details.\nIf you think this error should not appear, please report a bug.",
                               "Collecting Process Information Failed")
    }

    CollectProcessNodeTask(parentProcessId, project, onSuccess, onFinished, onThrowable).queue()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}