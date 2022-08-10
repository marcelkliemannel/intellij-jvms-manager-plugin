package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm

import com.intellij.openapi.project.Project
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjvmsmanagerplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.process.ProcessNode
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.*
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.ProcessTab
import java.util.*
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

class JvmProcessTab(project: Project,
                    showParentProcessDetails: (ProcessNode) -> Unit,
                    initialProcessNode: JvmProcessNode)
  : ProcessTab<JvmProcessNode>(project, showParentProcessDetails, initialProcessNode, "JVM") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val UNKNOWN_JVM_ENTRY_POINT_LABEL = JBLabel("Unknown")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val jvmEntryPointDescriptionLabel = JBLabel()
  private val jvmEntryPointValueWrapper = BorderLayoutPanel()
  private val jvmDebugAgentAddressDescriptionLabel = JBLabel("Debug agent:")
  private val jvmDebugAgentAddressValueLabel = JBLabel().copyable()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun JPanel.addAdditionalMainInformation(bag: GridBag) {
    add(jvmEntryPointDescriptionLabel, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.LARGE_VGAP))
    add(jvmEntryPointValueWrapper, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideTopInset(UIUtil.LARGE_VGAP).weightx(1.0).fillCellHorizontally())

    add(jvmDebugAgentAddressDescriptionLabel, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(jvmDebugAgentAddressValueLabel, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideTopInset(UIUtil.DEFAULT_VGAP).weightx(1.0).fillCellHorizontally())

    add(HyperlinkLabel("Show system properties").also { hyperLinkLabel ->
      hyperLinkLabel.addHyperlinkListener(showSystemProperties(hyperLinkLabel))
    }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).weightx(1.0).fillCellHorizontally())

    processNodeUpdated()
  }

  override fun processNodeUpdated() {
    super.processNodeUpdated()

    // Entry point
    jvmEntryPointValueWrapper.removeAll()
    val entryPoint = processNode.entryPoint
    if (entryPoint != null) {
      jvmEntryPointDescriptionLabel.text = "${entryPoint.typeTitle}:"
      jvmEntryPointValueWrapper.addToCenter(HyperlinkLabel(entryPoint.shortName).also { hyperLinkLabel ->
        hyperLinkLabel.addHyperlinkListener(showEntryPointFullName({ entryPoint }, hyperLinkLabel))
      })
    }
    else {
      jvmEntryPointDescriptionLabel.text = "Entry point:"
      jvmEntryPointValueWrapper.addToCenter(UNKNOWN_JVM_ENTRY_POINT_LABEL)
    }

    // Debugger port
    val debugAgentAddress = processNode.debugAgentAddress
    if (debugAgentAddress == null) {
      jvmDebugAgentAddressDescriptionLabel.isVisible = false
      jvmDebugAgentAddressValueLabel.isVisible = false
    }
    else {
      jvmDebugAgentAddressDescriptionLabel.isVisible = true
      jvmDebugAgentAddressValueLabel.isVisible = true
      jvmDebugAgentAddressValueLabel.text = debugAgentAddress.toString()
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun showEntryPointFullName(entryPoint: () -> JvmProcessNode.EntryPoint, hyperLinkLabel: HyperlinkLabel): (e: HyperlinkEvent) -> Unit = {
    val entryPointValue = entryPoint()
    TextPopup.showAbove("${entryPointValue.typeTitle} of PID ${processNode.process.processID}", entryPointValue.fullName, hyperLinkLabel)
  }

  private fun showSystemProperties(hyperLinkLabel: HyperlinkLabel): (e: HyperlinkEvent) -> Unit = {
    hyperLinkLabel.isEnabled = false
    val onSuccess: (Properties) -> Unit = { systemProperties ->
      val columnNames = arrayOf("Key", "Value")
      val data = systemProperties.map { arrayOf(it.key?.toString() ?: "", it.value?.toString() ?: "") }.sortedBy { it[0] }.toTypedArray()
      TablePopup.showAbove("System properties of PID ${processNode.process.processID}", data, columnNames, "System Property", "System Properties", hyperLinkLabel)
    }
    val onFinished = { hyperLinkLabel.isEnabled = true }
    CollectSystemPropertiesTask(project, processNode, onSuccess, onFinished).queue()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}