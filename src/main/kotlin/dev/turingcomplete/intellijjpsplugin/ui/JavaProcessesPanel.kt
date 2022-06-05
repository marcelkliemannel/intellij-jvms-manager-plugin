package dev.turingcomplete.intellijjpsplugin.ui

import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CollapseAllAction
import com.intellij.ide.actions.ExpandAllAction
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBSplitter
import com.intellij.ui.ScrollPaneFactory
import dev.turingcomplete.intellijjpsplugin.jps.JavaProcessNode
import dev.turingcomplete.intellijjpsplugin.jps.JavaProcessesCollectionTask
import dev.turingcomplete.intellijjpsplugin.ui.list.JavaProcessesTable
import javax.swing.JComponent
import javax.swing.JPanel

class JavaProcessesPanel(private val project: Project?) : SimpleToolWindowPanel(false) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val javaProcessesCollectionTask = createJavaProcessesCollectionTask()

  private val processesTable = JavaProcessesTable { collectProcesses() }

  private var collectProcessesInProgress = false

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    toolbar = createToolbar(this)
    setContent(createContent())
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createToolbar(targetComponent: JComponent): JComponent {
    val toolbarGroup = DefaultActionGroup(createReloadAction(),
                                          ExpandAllAction { processesTable.treeExpander },
                                          CollapseAllAction { processesTable.treeExpander })

    return ActionManager.getInstance()
            .createActionToolbar("${JavaProcessesToolWindowFactory.TOOLBAR_PLACE_PREFIX}.processes", toolbarGroup, false)
            .run {
              setTargetComponent(targetComponent)
              component
            }
  }

  private fun createReloadAction(): AnAction {
    return object : DumbAwareAction("Reload", null, AllIcons.Actions.Refresh) {

      override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = !collectProcessesInProgress
      }

      override fun actionPerformed(e: AnActionEvent) {
        collectProcesses()
      }
    }
  }

  private fun createContent(): JComponent {
    return JBSplitter(0.8f).apply {
      firstComponent = ScrollPaneFactory.createScrollPane(processesTable, true)
      secondComponent = JPanel()
    }
  }

  private fun collectProcesses() {
    if (collectProcessesInProgress) {
      return
    }

    collectProcessesInProgress = true
    processesTable.isEnabled = false

    javaProcessesCollectionTask.queue()
  }

  private fun createJavaProcessesCollectionTask(): JavaProcessesCollectionTask {
    val onSuccess: (List<JavaProcessNode>) -> Unit = { javaProcessNodes ->
      processesTable.setJavaProcessNodes(javaProcessNodes)
    }

    val onFinished = {
      collectProcessesInProgress = false
      processesTable.isEnabled = true
    }

    return JavaProcessesCollectionTask(project, onSuccess, onFinished)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}