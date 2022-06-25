package dev.turingcomplete.intellijjpsplugin.ui.detail

import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory.createScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI.emptyInsets
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.action.ActionUtils.SELECTED_PROCESS
import dev.turingcomplete.intellijjpsplugin.ui.action.ActionUtils.SELECTED_PROCESSES
import javax.swing.JComponent
import kotlin.properties.Delegates

open class ProcessNodeDetails<T : ProcessNode>(protected val project: Project,
                                               protected val showParentProcessDetails: (ProcessNode) -> Unit,
                                               protected val processTerminated: () -> Unit,
                                               initialProcessNode: T) : DataProvider {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  var processNode: T by Delegates.observable(initialProcessNode) { _, old, new ->
    if (old != new) {
      processNodeUpdated()
    }
  }

  private val tabbedPane = JBTabbedPane()
  private val tabs: List<DetailTab<T>> by lazy { createTabs() }
  val component: JComponent by lazy { createComponent() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  protected open fun createTabs(): List<DetailTab<T>> {
    return listOf(ProcessTab(project, showParentProcessDetails, processTerminated, processNode))
  }

  override fun getData(dataId: String): Any? {
    return when {
      SELECTED_PROCESSES.`is`(dataId) -> listOf(processNode)
      SELECTED_PROCESS.`is`(dataId) -> processNode
      else -> null
    }
  }

  fun setEnabled(enabled: Boolean) {
    component.isEnabled = enabled
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createComponent(): JComponent {
    return BorderLayoutPanel().apply {
      addToCenter(tabbedPane.apply {
        tabComponentInsets = emptyInsets()

        tabs.forEach { addTab(it.title, null, createScrollPane(it.createComponent(), true)) }
      })
    }
  }

  private fun processNodeUpdated() {
    tabs.forEach { it.processNode = processNode }
    tabbedPane.selectedIndex = 0
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}