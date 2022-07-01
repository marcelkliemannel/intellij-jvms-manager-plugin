package dev.turingcomplete.intellijjvmsmanagerplugin.ui

import com.intellij.ide.impl.dataRules.GetDataRule
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode

object CommonsDataKeys {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val SELECTED_PROCESSES_DATA_KEY: DataKey<List<ProcessNode>> = DataKey.create("JavaProcessesPlugin.SelectedProcesses")

  /**
   * The data key is also hard coded for the [CurrentProcessDetailsDataRule] in
   * the `plugin.xml`.
   */
  val CURRENT_PROCESS_DETAILS_DATA_KEY: DataKey<ProcessNode> = DataKey.create("dev.turingcomplete.intellijjvmsmanagerplugin.currentProcessDetails")

  /**
   * The data key is also hard coded for the
   * [CollectJvmProcessNodesTaskRunningDataRule] in the `plugin.xml`.
   */
  val COLLECT_JVM_PROCESS_NODES_TASK_RUNNING: DataKey<Boolean> = DataKey.create("dev.turingcomplete.intellijjvmsmanagerplugin.collectJvmProcessNodesTaskRunning")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun <T> getRequiredData(dataKey: DataKey<T>, dataContext: DataContext): T {
    return dataContext.getData(dataKey)
           ?: throw IllegalStateException("snh: Data context is missing required data for key: ${dataKey.name}")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class CurrentProcessDetailsDataRule : GetDataRule {

    override fun getData(dataProvider: DataProvider): Any? {
      return dataProvider.getData(CURRENT_PROCESS_DETAILS_DATA_KEY.name)
             ?: JvmsManagerToolWindowFactory.getData(dataProvider, CURRENT_PROCESS_DETAILS_DATA_KEY)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class CollectJvmProcessNodesTaskRunningDataRule : GetDataRule {

    override fun getData(dataProvider: DataProvider): Any? {
      return dataProvider.getData(COLLECT_JVM_PROCESS_NODES_TASK_RUNNING.name)
             ?: JvmsManagerToolWindowFactory.getData(dataProvider, COLLECT_JVM_PROCESS_NODES_TASK_RUNNING)
    }
  }
}