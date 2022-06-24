package dev.turingcomplete.intellijjpsplugin.ui.detail.jvm

import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijjpsplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.detail.DetailTab
import dev.turingcomplete.intellijjpsplugin.ui.detail.ProcessNodeDetails

class JvmProcessNodeDetails(project: Project,
                            showParentProcessDetails: (ProcessNode) -> Unit,
                            processTerminated: () -> Unit,
                            initialProcessNode: JvmProcessNode)
  : ProcessNodeDetails<JvmProcessNode>(project, showParentProcessDetails, processTerminated, initialProcessNode) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createTabs(): List<DetailTab<JvmProcessNode>> {
    return listOf(JvmProcessTab(project, showParentProcessDetails, processTerminated, processNode), JvmActionsTab(project, processNode))
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}