package dev.turingcomplete.intellijjpsplugin.ui.detail.jvm

import com.intellij.openapi.project.Project
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijjpsplugin.process.JvmProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.ui.common.TablePopup
import dev.turingcomplete.intellijjpsplugin.ui.common.TextPopup
import dev.turingcomplete.intellijjpsplugin.ui.common.overrideTopInset
import dev.turingcomplete.intellijjpsplugin.ui.detail.ProcessTab
import java.util.*
import javax.swing.JPanel
import javax.swing.event.HyperlinkEvent

class JvmProcessTab(project: Project,
                    showParentProcessDetails: (ProcessNode) -> Unit,
                    initialProcessNode: JvmProcessNode)
  : ProcessTab<JvmProcessNode>(project, showParentProcessDetails, initialProcessNode, "JVM") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val entryPointWrapper = BorderLayoutPanel()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun JPanel.addAdditionalMainInformation(bag: GridBag) {
    add(entryPointWrapper, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.LARGE_VGAP).weightx(1.0).fillCellHorizontally())

    add(HyperlinkLabel("Show system properties").also { hyperLinkLabel ->
      hyperLinkLabel.addHyperlinkListener(showSystemProperties(hyperLinkLabel))
    }, bag.nextLine().next().coverLine().overrideTopInset(UIUtil.DEFAULT_VGAP).weightx(1.0).fillCellHorizontally())

    processNodeUpdated()
  }

  override fun processNodeUpdated() {
    super.processNodeUpdated()

    entryPointWrapper.removeAll()
    val entryPoint = processNode.entryPoint
    if (entryPoint != null) {
      entryPointWrapper.addToCenter(HyperlinkLabel("${entryPoint.typeTitle}: ${entryPoint.shortName}").also { hyperLinkLabel ->
        hyperLinkLabel.addHyperlinkListener(showEntryPointFullName({ entryPoint }, hyperLinkLabel))
      })
    }
    else {
      entryPointWrapper.addToCenter(JBLabel("JVM entry point unknown"))
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