package dev.turingcomplete.intellijjpsplugin.ui.common

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.table.JBTable
import com.intellij.vcs.commit.NonModalCommitPanel.Companion.showAbove
import java.awt.Dimension
import java.awt.datatransfer.StringSelection
import javax.swing.JComponent
import javax.swing.table.DefaultTableModel

class TablePopup(data: Array<Array<String>>,
                 columnNames: Array<String>,
                 singleDataName: String,
                 pluralDataName: String,
                 copySeparator: String) : JBTable(MyTableModel(data, columnNames)), DataProvider {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val SELECTED_VALUES: DataKey<List<List<String>>> = DataKey.create("JavaProcessesPlugin.SelectedValues")

    fun showAbove(title: String,
                  data: Array<Array<String>>,
                  columnNames: Array<String>,
                  singleDataName: String,
                  pluralDataName: String,
                  target: JComponent,
                  copySeparator: String = "=") {

      val tablePopup = ScrollPaneFactory.createScrollPane(TablePopup(data, columnNames, singleDataName, pluralDataName, copySeparator), true).apply {
        val width = if (columnNames.size < 4) 500 else 650
        val height = if (data.size < 10) 200 else 400
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
              .setShowBorder(false)
              .setCancelKeyEnabled(true)
              .setCancelOnClickOutside(true)
              .setCancelOnOtherWindowOpen(false)
              .createPopup()
              .showAbove(target)
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    val contextMenuActions = DefaultActionGroup(CopyValues(singleDataName, pluralDataName, copySeparator))
    addMouseListener(UiUtils.Table.createContextMenuMouseListener(this::class.qualifiedName!!) {
      contextMenuActions
    })
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun getData(dataId: String): Any? {
    return when {
      SELECTED_VALUES.`is`(dataId) -> {
        val dataVector = (model as DefaultTableModel).dataVector
        return selectionModel.selectedIndices.map { dataVector[it] }
      }
      else -> null
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MyTableModel(data: Array<Array<String>>, columnNames: Array<String>)
    : DefaultTableModel(data, columnNames) {

    override fun isCellEditable(row: Int, column: Int): Boolean {
      return false
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class CopyValues(private val singleDataName: String,
                   private val pluralDataName: String,
                   private val copySeparator: String)
    : DumbAwareAction("Copy Properties", null, AllIcons.Actions.Copy) {

    override fun update(e: AnActionEvent) {
      val selectedProperties = SELECTED_VALUES.getData(e.dataContext) ?: throw IllegalStateException("snh: Data missing")

      e.presentation.isVisible = selectedProperties.isNotEmpty()
      e.presentation.text = if (selectedProperties.size > 1) {
        "Copy ${selectedProperties.size} $pluralDataName"
      }
      else {
        "Copy $singleDataName"
      }
    }

    override fun actionPerformed(e: AnActionEvent) {
      val selectedProperties = SELECTED_VALUES.getData(e.dataContext) ?: throw IllegalStateException("snh: Data missing")
      if (selectedProperties.isEmpty()) {
        return
      }

      val textToCopy = selectedProperties.joinToString("\n") { it.joinToString(copySeparator) }
      CopyPasteManager.getInstance().setContents(StringSelection(textToCopy))
    }
  }
}