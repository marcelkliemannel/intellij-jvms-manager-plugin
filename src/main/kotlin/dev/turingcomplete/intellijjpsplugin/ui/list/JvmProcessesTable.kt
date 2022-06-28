package dev.turingcomplete.intellijjpsplugin.ui.list

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.treeStructure.treetable.DefaultTreeTableExpander
import com.intellij.ui.treeStructure.treetable.ListTreeTableModelOnColumns
import com.intellij.ui.treeStructure.treetable.TreeColumnInfo
import com.intellij.ui.treeStructure.treetable.TreeTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.tree.TreeUtil
import dev.turingcomplete.intellijjpsplugin.JpsPluginService
import dev.turingcomplete.intellijjpsplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.CommonsDataKeys.SELECTED_PROCESSES_DATA_KEY
import dev.turingcomplete.intellijjpsplugin.ui.action.ForciblyTerminateProcessesAction
import dev.turingcomplete.intellijjpsplugin.ui.action.GracefullyTerminateProcessesAction
import dev.turingcomplete.intellijjpsplugin.ui.action.TotalResidentSetSizeAction
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import javax.swing.JTable
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode

class JvmProcessesTable(private val project: Project)
  : TreeTable(ListTreeTableModelOnColumns(DefaultMutableTreeNode(), createProcessesTableColumns())), DataProvider {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private fun createProcessesTableColumns(): Array<ColumnInfo<Any, out Any>> {
      return arrayOf(TreeColumnInfo("PID"),
                     ProcessNodeColumnInfo("Name", { it.smartName }),
                     ProcessNodeColumnInfo("RSS", { StringUtil.formatFileSize(it.process.residentSetSize) },
                                           "The resident set size (RSS) shows how much memory is allocated to this process and is in RAM."),
                     ProcessNodeColumnInfo("Uptime", { StringUtil.formatDuration(it.process.upTime) }))
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val createContextMenuActions : ActionGroup by lazy { createContextMenuActions() }
  val treeExpander = DefaultTreeTableExpander(this)

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    setTreeCellRenderer(MyTreeTableCellRenderer())
    setDefaultRenderer(String::class.java, MyTableCellRenderer())

    addMouseListener(UiUtils.Table.createContextMenuMouseListener(this@JvmProcessesTable::class.qualifiedName!!) {
      createContextMenuActions
    })

    selectionModel.addListSelectionListener {
      // This listener gets called for every row during the refresh of this
      // table. We prevent this by only reacting to events that have been
      // coming from an enabled table.
      if (it.valueIsAdjusting || !isEnabled) {
        return@addListSelectionListener
      }

      // Using `it.firstIndex` or `it.lastIndex` as an indicator for the
      // selected row does not work in any situation. If the row selection
      // gets changed by the keyboard, the `firstIndex` may be the old row
      // and the `lastIndex` one the new one. But we cannot distinguish this
      // case  from a multi selection.
      val selectedIndices = selectionModel.selectedIndices
      if (selectedIndices.size == 1) {
        tree.getPathForRow(selectedIndices[0])?.let { selectedPath ->
          val selectedProcessNode = selectedPath.lastPathComponent as ProcessNode
          project.getService(JpsPluginService::class.java).showProcessDetails(selectedProcessNode)
        }
      }
    }

    columnModel.getColumn(0).preferredWidth = 110
    columnModel.getColumn(1).preferredWidth = 400
    columnModel.getColumn(2).preferredWidth = 60
    columnModel.getColumn(3).preferredWidth = 130

    tableHeader.resizingAllowed = true

    tree.showsRootHandles = true
    tree.isRootVisible = false

    emptyText.apply {
      appendLine("No processes found")
      appendLine("Reload", SimpleTextAttributes.LINK_ATTRIBUTES) {
        project.getService(JpsPluginService::class.java)
      }
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun setJvmProcessNodes(jvmProcessNodes: List<JvmProcessNode>) {
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
      SELECTED_PROCESSES_DATA_KEY.`is`(dataId) -> TreeUtil.collectSelectedPaths(this.tree).map { it.lastPathComponent }.filterIsInstance<ProcessNode>()
      else -> null
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createContextMenuActions(): ActionGroup {
    return DefaultActionGroup().apply {
      add(TotalResidentSetSizeAction())
      addSeparator()
      add(GracefullyTerminateProcessesAction(collectJavaProcessesOnSuccess = true))
      add(ForciblyTerminateProcessesAction(collectJavaProcessesOnSuccess = true))
    }
  }

  fun processDetailsUpdated(processNodes: List<ProcessNode>) {
    val treeModel = tableModel as ListTreeTableModelOnColumns
    processNodes.forEach { treeModel.reload(it) }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ProcessNodeColumnInfo(title: String, val getValue: (ProcessNode) -> String, val toolTipText: String? = null)
    : ColumnInfo<Any, String>(title) {

    override fun valueOf(node: Any): String = if (node is ProcessNode) getValue(node) else node.toString()

    override fun getTooltipText(): String? = toolTipText
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MyTableCellRenderer : ColoredTableCellRenderer() {

    override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
      append(value as String)
    }
  }

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