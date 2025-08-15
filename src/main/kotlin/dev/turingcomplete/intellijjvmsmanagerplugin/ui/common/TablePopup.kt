package dev.turingcomplete.intellijjvmsmanagerplugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.UiUtils.Table.createNonEditableDataModel
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import javax.swing.JComponent
import javax.swing.table.DefaultTableModel

class TablePopup(
  private val title: String,
  data: Array<Array<String>>,
  columnNames: Array<String>,
  singleDataName: String,
  pluralDataName: String,
  joinRowValues: (List<String>) -> String = { it.joinToString("=") },
) : JBTable(createNonEditableDataModel(data, columnNames)), DataProvider {

  // -- Companion Object ---------------------------------------------------- //

  companion object {
    val SELECTED_VALUES: DataKey<List<List<String>>> =
      DataKey.create("JavaProcessesPlugin.SelectedValues")
  }

  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //

  init {
    val contextMenuActions =
      DefaultActionGroup(CopyValues(singleDataName, pluralDataName, joinRowValues))
    addMouseListener(
      UiUtils.Table.createContextMenuMouseListener(this::class.qualifiedName!!) {
        contextMenuActions
      }
    )
  }

  // -- Exported Methods ---------------------------------------------------- //

  fun showAbove(target: JComponent) {
    val tablePopup =
      ScrollPaneFactory.createScrollPane(this, true).apply {
        val width = if (dataModel.columnCount < 4) 600 else 700
        val height = if (dataModel.rowCount < 10) 200 else 400
        preferredSize = Dimension(width, height)
      }

    JBPopupFactory.getInstance()
      .createComponentPopupBuilder(tablePopup, tablePopup)
      .setRequestFocus(true)
      .setTitle(title)
      .setFocusable(true)
      .setResizable(true)
      .setMovable(true)
      .setModalContext(false)
      .setShowShadow(true)
      .setShowBorder(true)
      .setCancelKeyEnabled(true)
      .setCancelOnClickOutside(true)
      .setCancelOnOtherWindowOpen(false)
      .createPopup()
      .show(target)
  }

  override fun getData(dataId: String): Any? {
    return when {
      SELECTED_VALUES.`is`(dataId) -> {
        val dataVector = (model as DefaultTableModel).dataVector
        return selectionModel.selectedIndices.map { dataVector[it] }
      }

      else -> null
    }
  }

  // -- Private Methods
  // ---------------------------------------------------------------------------------e------------
  // //
  // -- Inner Type ---------------------------------------------------------- //

  private class CopyValues(
    private val singleDataName: String,
    private val pluralDataName: String,
    private val joinRowValues: (List<String>) -> String,
  ) : DumbAwareAction("Copy Properties", null, AllIcons.Actions.Copy) {

    override fun update(e: AnActionEvent) {
      val selectedProperties =
        SELECTED_VALUES.getData(e.dataContext) ?: throw IllegalStateException("snh: Data missing")

      e.presentation.isVisible = selectedProperties.isNotEmpty()
      e.presentation.text =
        if (selectedProperties.size > 1) {
          "Copy ${selectedProperties.size} $pluralDataName"
        } else {
          "Copy $singleDataName"
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.EDT

    override fun actionPerformed(e: AnActionEvent) {
      val selectedProperties: List<List<String>> =
        SELECTED_VALUES.getData(e.dataContext) ?: throw IllegalStateException("snh: Data missing")
      if (selectedProperties.isEmpty()) {
        return
      }

      val textToCopy = selectedProperties.joinToString("\n") { joinRowValues(it) }
      CopyPasteManager.getInstance().setContents(StringSelection(textToCopy))
    }
  }
}
