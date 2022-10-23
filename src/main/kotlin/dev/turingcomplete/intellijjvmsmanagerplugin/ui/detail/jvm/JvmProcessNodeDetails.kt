package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijjvmsmanagerplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.DetailTab
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.ProcessNodeDetails
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmActionsTab

class JvmProcessNodeDetails(project: Project,
                            showParentProcessDetails: (ProcessNode) -> Unit,
                            initialProcessNode: JvmProcessNode,
                            parent: Disposable)
  : ProcessNodeDetails<JvmProcessNode>(project, showParentProcessDetails, initialProcessNode, parent) {
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