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
import com.intellij.ui.JBSplitter
import com.intellij.ui.ScrollPaneFactory
import dev.turingcomplete.intellijjpsplugin.process.CollectJavaProcessNodesTask
import dev.turingcomplete.intellijjpsplugin.process.CollectProcessNodeTask
import dev.turingcomplete.intellijjpsplugin.process.JavaProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.detail.ProcessDetailPanel
import dev.turingcomplete.intellijjpsplugin.ui.list.JavaProcessesTable
import javax.swing.JComponent

class JavaProcessesPanel(private val project: Project?) : SimpleToolWindowPanel(false), Disposable {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(JavaProcessesPanel::class.java)
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val javaProcessesCollectionTask = createJavaProcessesCollectionTask()

  private var contentSplitter = JBSplitter(0.75f)
  private val processesTable: JavaProcessesTable

  private var collectJavaProcessesInProgress = false

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    toolbar = createToolbar()

    processesTable = JavaProcessesTable({ collectJavaProcesses() }, { showProcessNodeDetails(it) })

    setContent(contentSplitter.apply {
      firstComponent = ScrollPaneFactory.createScrollPane(processesTable, true)
      secondComponent = ProcessDetailPanel.NO_PROCESS_SELECTED
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
    val onSuccess: (List<JavaProcessNode>) -> Unit = { javaProcessNodes ->
      processesTable.setJavaProcessNodes(javaProcessNodes)
    }

    val onFinished = {
      collectJavaProcessesInProgress = false
      processesTable.isEnabled = true
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

  private fun showProcessNodeDetails(processNode: ProcessNode) {
    contentSplitter.secondComponent = ProcessDetailPanel(processNode) {
      val parentProcessNode = processNode.parent
      if (parentProcessNode is ProcessNode) {
        showProcessNodeDetails(parentProcessNode)
      }
      else {
        collectParentProcessNodeDetails(processNode)
      }
    }
  }

  private fun collectParentProcessNodeDetails(processNode: ProcessNode) {
    contentSplitter.secondComponent.isEnabled = false

    val parentProcessId = processNode.process.parentProcessID

    val onSuccess: (ProcessNode?) -> Unit = { parentProcessNode ->
      if (parentProcessNode != null) {
        showProcessNodeDetails(parentProcessNode)
        processNode.setParent(parentProcessNode)
      }
      else {
        Messages.showInfoMessage(project, "Unable to find information about process with PID $parentProcessId.", "Collecting Process Information")
      }
    }

    val onFinished = {
      contentSplitter.secondComponent.isEnabled = true
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