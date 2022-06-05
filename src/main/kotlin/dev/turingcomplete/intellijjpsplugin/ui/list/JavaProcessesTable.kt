package dev.turingcomplete.intellijjpsplugin.ui.list

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DataKey
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
import dev.turingcomplete.intellijjpsplugin.jps.JavaProcessNode
import dev.turingcomplete.intellijjpsplugin.jps.ProcessNode
import dev.turingcomplete.intellijjpsplugin.jps.action.ForciblyTerminateProcessAction
import dev.turingcomplete.intellijjpsplugin.jps.action.GracefullyTerminateProcessAction
import dev.turingcomplete.intellijjpsplugin.ui.UiUtils
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class JavaProcessesTable(val recollectProcesses: () -> Unit)
  : TreeTable(ListTreeTableModelOnColumns(DefaultMutableTreeNode(), createProcessesTableColumns())), DataProvider {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    val SELECTED_PROCESSES: DataKey<List<ProcessNode>> = DataKey.create("JavaProcessesPlugin.SelectedProcesses")

    private fun createProcessesTableColumns(): Array<ColumnInfo<Any, out Any>> {
      return arrayOf(TreeColumnInfo("PID"),
                     ProcessNodeColumnInfo("Name") { it.displayName() },
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

    columnModel.getColumn(0).preferredWidth = 40
    columnModel.getColumn(1).preferredWidth = 400

    tableHeader.resizingAllowed = true

    tree.showsRootHandles = true
    tree.isRootVisible = false

    emptyText.text = "No Java processes found"
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun setJavaProcessNodes(javaProcessNodes: List<JavaProcessNode>) {
    val oldExpandedPaths = TreeUtil.collectExpandedPaths(tree)

    // It's important that we reuse the root node to make `restoreExpandedPaths`
    // work, because the `TreePath` uses `equal()` to match the old/new nodes.
    val treeModel = tableModel as ListTreeTableModelOnColumns
    val root = treeModel.root as DefaultMutableTreeNode
    root.removeAllChildren()
    javaProcessNodes.forEach { root.add(it) }
    treeModel.reload()

    TreeUtil.restoreExpandedPaths(tree, oldExpandedPaths)
  }

  override fun getData(dataId: String): Any? {
    if (SELECTED_PROCESSES.`is`(dataId)) {
      return TreeUtil.collectSelectedPaths(this.tree).map { it.lastPathComponent }.filterIsInstance<ProcessNode>()
    }

    return null
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createContextMenuActions(): ActionGroup {
    return DefaultActionGroup().apply {
      add(GracefullyTerminateProcessAction().onFinished { recollectProcesses() })
      add(ForciblyTerminateProcessAction().onFinished { recollectProcesses() })
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

      icon = value.processType?.icon
      append(value.process.processID.toString())
      toolTipText = value.processType?.description

      val isJavaProcess = value is JavaProcessNode
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