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
import java.nio.file.Path
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
    private val NO_JAVA_AGENT_ATTACHED = JBLabel("None attached")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val jvmEntryPointDescriptionLabel = JBLabel()
  private val jvmEntryPointValueWrapper = BorderLayoutPanel()
  private val jvmDebugAgentAddressDescriptionLabel = JBLabel("Debug agent:")
  private val jvmDebugAgentAddressValueLabel = JBLabel().copyable()
  private val javaAgentsLabel = JBLabel("Java agents:")
  private val javaAgentsWrapper = BorderLayoutPanel()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun JPanel.addAdditionalMainInformation(bag: GridBag) {
    add(jvmEntryPointDescriptionLabel, bag.nextLine().next().overrideTopInset(UIUtil.LARGE_VGAP))
    add(jvmEntryPointValueWrapper, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideTopInset(UIUtil.LARGE_VGAP).weightx(1.0).fillCellHorizontally())
    add(jvmDebugAgentAddressDescriptionLabel, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(jvmDebugAgentAddressValueLabel, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideTopInset(UIUtil.DEFAULT_VGAP).weightx(1.0).fillCellHorizontally())
    add(javaAgentsLabel, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_VGAP))
    add(javaAgentsWrapper, bag.next().overrideLeftInset(UIUtil.DEFAULT_HGAP / 2).overrideTopInset(UIUtil.DEFAULT_VGAP).weightx(1.0).fillCellHorizontally())

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

    // Java agents
    javaAgentsWrapper.removeAll()
    val numOfJavaAgents = processNode.javaAgents.size
    if (numOfJavaAgents == 0) {
      javaAgentsWrapper.addToCenter(NO_JAVA_AGENT_ATTACHED)
    }
    else {
      javaAgentsWrapper.addToCenter(HyperlinkLabel("$numOfJavaAgents attached").also { hyperLinkLabel ->
        hyperLinkLabel.addHyperlinkListener(showAttachedJavaAgents(hyperLinkLabel))
      })
    }

    // Debugger port
    val debugAgentAddress = processNode.debugAgentAddress
    if (debugAgentAddress == null) {
      jvmDebugAgentAddressValueLabel.text = "Not available"
    }
    else {
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
      TablePopup("System properties of PID ${processNode.process.processID}", data, columnNames, "System Property", "System Properties")
              .showAbove(hyperLinkLabel)
    }
    val onFinished = { hyperLinkLabel.isEnabled = true }
    CollectSystemPropertiesTask(project, processNode, onSuccess, onFinished).queue()
  }

  private fun showAttachedJavaAgents(hyperLinkLabel: HyperlinkLabel): (e: HyperlinkEvent) -> Unit = {
    val columnNames = arrayOf("Name", "Path", "Options")
    val data = processNode.javaAgents.map {
      arrayOf(Path.of(it.key).fileName.toString(), it.key, it.value ?: "")
    }.sortedBy { it[0] }.toTypedArray()
    TablePopup("Java agents of PID ${processNode.process.processID}", data, columnNames, "Java Agent", "Java Agents") {
      "-javaAgent:${it.drop(0).joinToString("=")}"
    }.showAbove(hyperLinkLabel)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}