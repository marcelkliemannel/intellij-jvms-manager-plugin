package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.ui.ScrollPaneFactory.createScrollPane
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI.emptyInsets
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.CommonsDataKeys.CURRENT_PROCESS_DETAILS_DATA_KEY
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.CommonsDataKeys.SELECTED_PROCESSES_DATA_KEY
import javax.swing.JComponent
import kotlin.properties.Delegates

open class ProcessNodeDetails<T : ProcessNode>(
  protected val project: Project,
  protected val showParentProcessDetails: (ProcessNode) -> Unit,
  initialProcessNode: T,
  protected val parent: Disposable,
) : DataProvider {

  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //

  var processNode: T by
    Delegates.observable(initialProcessNode) { _, old, new ->
      if (old != new) {
        tabs.forEach { it.processNode = processNode }
        tabbedPane.selectedIndex = 0
        (tabbedPane.selectedComponent as? JBScrollPane)?.verticalScrollBar?.value = 0
      }
    }

  private val tabbedPane = JBTabbedPane()
  private val tabs: List<DetailTab<T>> by lazy { createTabs() }
  val component: JComponent by lazy { createComponent(parent) }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  protected open fun createTabs(): List<DetailTab<T>> {
    return listOf(ProcessTab(project, showParentProcessDetails, processNode))
  }

  override fun getData(dataId: String): Any? {
    return when {
      SELECTED_PROCESSES_DATA_KEY.`is`(dataId) -> listOf(processNode)
      CURRENT_PROCESS_DETAILS_DATA_KEY.`is`(dataId) -> processNode
      else -> null
    }
  }

  fun setEnabled(enabled: Boolean) {
    component.isEnabled = enabled
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createComponent(parent: Disposable): JComponent {
    return object : BorderLayoutPanel(), DataProvider {

      init {
        addToCenter(
          tabbedPane.apply {
            tabComponentInsets = emptyInsets()

            tabs.forEach {
              val tabComponent =
                it.createComponent(parent).apply {
                  UIUtil.setBackgroundRecursively(this, UIUtil.getTreeBackground())
                  UIUtil.setForegroundRecursively(this, UIUtil.getTreeForeground())
                }
              addTab(it.title, null, createScrollPane(tabComponent, true))
            }
          }
        )
      }

      override fun getData(dataId: String): Any? =
        when {
          CURRENT_PROCESS_DETAILS_DATA_KEY.`is`(dataId) -> processNode
          else -> null
        }
    }
  }

  fun processDetailsUpdated() {
    tabs[tabbedPane.selectedIndex].processNodeUpdated()
  }

  // -- Inner Type ---------------------------------------------------------- //
}
