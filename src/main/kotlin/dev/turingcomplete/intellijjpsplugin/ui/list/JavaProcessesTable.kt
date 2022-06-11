package dev.turingcomplete.intellijjpsplugin.ui.list

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.treeStructure.treetable.DefaultTreeTableExpander
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.tree.TreeUtil
import dev.turingcomplete.intellijjpsplugin.process.jvm.JvmProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.process.action.ForciblyTerminateProcessesAction
import dev.turingcomplete.intellijjpsplugin.process.action.GracefullyTerminateProcessesAction
import dev.turingcomplete.intellijjpsplugin.process.action.ProcessNodeActionUtils.SELECTED_PROCESS
import dev.turingcomplete.intellijjpsplugin.process.action.ProcessNodeActionUtils.SELECTED_PROCESSES
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class JavaProcessesTable(val collectProcesses: () -> Unit, val showProcessNodeDetails: (ProcessNode) -> Unit)
  : TreeTable(ListTreeTableModelOnColumns(DefaultMutableTreeNode(), createProcessesTableColumns())), DataProvider {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private fun createProcessesTableColumns(): Array<ColumnInfo<Any, out Any>> {
      return arrayOf(TreeColumnInfo("PID"),
                     ProcessNodeColumnInfo("Name") { it.smartName },
                     ProcessNodeColumnInfo("Uptime") { StringUtil.formatDuration(it.process.upTime) })
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val createContextMenuActions : ActionGroup by lazy { createContextMenuActions() }
  val treeExpander = DefaultTreeTableExpander(this)

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    setTreeCellRenderer(MyTreeTableCellRenderer())

    addMouseListener(UiUtils.Table.createContextMenuMouseListener(this@JavaProcessesTable::class.qualifiedName!!) {
      createContextMenuActions
    })

    selectionModel.addListSelectionListener {
      if (it.valueIsAdjusting) {
        return@addListSelectionListener
      }

      TreeUtil.getSelectedPathIfOne(tree)?.let { slectedPath -> showProcessNodeDetails(slectedPath.lastPathComponent as ProcessNode) }
    }

    columnModel.getColumn(0).preferredWidth = 40
    columnModel.getColumn(1).preferredWidth = 400

    tableHeader.resizingAllowed = true

    tree.showsRootHandles = true
    tree.isRootVisible = false

    emptyText.text = "No Java processes found"
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun setJavaProcessNodes(jvmProcessNodes: List<JvmProcessNode>) {
    val oldExpandedPaths = TreeUtil.collectExpandedPaths(tree)

    // It's important that we reuse the root node to make `restoreExpandedPaths`
    // work, because the `TreePath` uses `equal()` to match the old/new nodes.
    val treeModel = tableModel as ListTreeTableModelOnColumns
    val root = treeModel.root as DefaultMutableTreeNode
    root.removeAllChildren()
    jvmProcessNodes.forEach { root.add(it) }
    treeModel.reload()

    TreeUtil.restoreExpandedPaths(tree, oldExpandedPaths)
  }

  override fun getData(dataId: String): Any? {
    return when {
      SELECTED_PROCESSES.`is`(dataId) -> TreeUtil.collectSelectedPaths(this.tree).map { it.lastPathComponent }.filterIsInstance<ProcessNode>()
      SELECTED_PROCESS.`is`(dataId) -> TreeUtil.getSelectedPathIfOne(tree)?.lastPathComponent
      else -> null
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createContextMenuActions(): ActionGroup {
    return DefaultActionGroup().apply {
      add(GracefullyTerminateProcessesAction().onFinished { collectProcesses() })
      add(ForciblyTerminateProcessesAction().onFinished { collectProcesses() })
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ProcessNodeColumnInfo(title: String, val getValue: (ProcessNode) -> String) : ColumnInfo<Any, String>(title) {

    override fun valueOf(node: Any): String {
      return if (node is ProcessNode) getValue(node) else node.toString()
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MyTreeTableCellRenderer : ColoredTreeCellRenderer() {

    override fun customizeCellRenderer(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean, leaf: Boolean, row: Int, hasFocus: Boolean) {
      if (value !is ProcessNode) {
        return
      }

      icon = value.processType.icon
      append(value.process.processID.toString())
      toolTipText = value.processType.description

      val isJavaProcess = value is JvmProcessNode
      if (!isJavaProcess) {
        append("*")
        if (toolTipText?.isNotBlank() == true) {
          toolTipText = "$toolTipText (*non-Java process)"
        }
        toolTipText = "*Non-Java process"
      }
    }
  }
}