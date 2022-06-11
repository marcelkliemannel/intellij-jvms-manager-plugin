package dev.turingcomplete.intellijjpsplugin.ui.detail.jvmaction

import com.intellij.ui.components.JBCheckBox
import com.intellij.unscramble.UnscrambleDialog
import dev.turingcomplete.intellijjpsplugin.ui.common.TextPopup
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import java.awt.GridBagLayout
import javax.swing.JPanel

class CreateThreadDumpAction : JvmAction("Create Thread Dump", "Creating Thread Dump", JTool.JSTACK) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val printAdditionalLocksInformation = JBCheckBox("Print additional information about locks", true)
  private val printAdditionalThreadsInformation = JBCheckBox("Print additional information about threads", true)
  private val openResultInIntelliJThreadDumpAnalyzer = JBCheckBox("Open result in IntelliJ thread dump analyzer", true)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createConfigurationComponent() = JPanel(GridBagLayout()).apply {
    border = UiUtils.EMPTY_BORDER

    val bag = UiUtils.createDefaultGridBag()

    add(printAdditionalLocksInformation, bag.nextLine().next().weightx(1.0).fillCellHorizontally())
    add(printAdditionalThreadsInformation, bag.nextLine().next().weightx(1.0).fillCellHorizontally())
    add(openResultInIntelliJThreadDumpAnalyzer, bag.nextLine().next().weightx(1.0).fillCellHorizontally())
  }

  override fun createActionParameters(): List<String> {
    val command = mutableListOf<String>()

    if (printAdditionalLocksInformation.isSelected) {
      command.add("-l")
    }
    if (printAdditionalThreadsInformation.isSelected) {
      command.add("-e")
    }
    command.add(PID_PLACEHOLDER)

    return command
  }

  override fun onSuccess(result: String?, jvmActionContext: JvmActionContext) {
    if (openResultInIntelliJThreadDumpAnalyzer.isSelected) {
      val unscrambleDialog = UnscrambleDialog(jvmActionContext.project)
      unscrambleDialog.setText(result ?: "")
      unscrambleDialog.show()
    }
    else {
      TextPopup.showCenteredInCurrentWindow("Thread Dump of PID ${jvmActionContext.processNode.process.processID}",
                                           result ?: "", jvmActionContext.project, false)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}