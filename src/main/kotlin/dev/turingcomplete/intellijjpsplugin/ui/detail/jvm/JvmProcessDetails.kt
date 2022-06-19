package dev.turingcomplete.intellijjpsplugin.ui.detail.jvm

import com.intellij.openapi.project.Project
import dev.turingcomplete.intellijjpsplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.detail.ProcessDetailTab
import dev.turingcomplete.intellijjpsplugin.ui.detail.ProcessDetails

class JvmProcessDetails(private val project: Project,
                        jvmProcessNode: JvmProcessNode,
                        showParentProcessDetails: (ProcessNode) -> Unit)
  : ProcessDetails<JvmProcessNode>(jvmProcessNode, showParentProcessDetails) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createAdditionalTabs(): Sequence<ProcessDetailTab> {
    return sequenceOf(JvmActionsTab(project, processNode))
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}