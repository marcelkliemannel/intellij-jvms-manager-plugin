package dev.turingcomplete.intellijjpsplugin.ui.detail.jvm

import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijjpsplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.detail.DetailTab
import dev.turingcomplete.intellijjpsplugin.ui.detail.ProcessNodeDetails
import dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.jvmaction.JvmActionsTab

class JvmProcessNodeDetails(project: Project,
                            showParentProcessDetails: (ProcessNode) -> Unit,
                            initialProcessNode: JvmProcessNode)
  : ProcessNodeDetails<JvmProcessNode>(project, showParentProcessDetails, initialProcessNode) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createTabs(): List<DetailTab<JvmProcessNode>> {
    return listOf(JvmProcessTab(project, showParentProcessDetails, processNode),
                  JvmActionsTab(project, processNode))
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}