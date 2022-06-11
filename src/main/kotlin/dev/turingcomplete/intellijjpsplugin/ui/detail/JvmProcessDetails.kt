package dev.turingcomplete.intellijjpsplugin.ui.detail

import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.process.jvm.JvmProcessNode
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

class JvmProcessDetails(jvmProcessNode: JvmProcessNode, showParentProcessDetails: (ProcessNode) -> Unit)
  : ProcessDetails<JvmProcessNode>(jvmProcessNode, showParentProcessDetails) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createAdditionalTabs(): Sequence<ProcessDetailTab> {
    return sequenceOf(JvmActionsPanel(processNode))
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class JvmActionsPanel(processNode: ProcessNode) : ProcessDetailTab("JVM Actions", processNode) {

    override fun createComponent(): JComponent {
      return JPanel(GridBagLayout()).apply {
      }
    }

    override fun processNodeUpdated() {
    }
  }
}