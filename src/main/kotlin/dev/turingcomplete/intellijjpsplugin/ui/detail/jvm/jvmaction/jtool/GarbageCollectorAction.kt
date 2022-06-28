package dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.jvmaction.jtool

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjpsplugin.ui.common.AnActionOptionButton
import dev.turingcomplete.intellijjpsplugin.ui.common.NotificationUtils
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjpsplugin.ui.common.overrideLeftInset
import dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.jvmaction.JvmAction
import dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.jvmaction.JvmActionContext
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JPanel

class GarbageCollectorAction : JvmAction("Garbage Collector") {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createComponent() = JPanel(GridBagLayout()).apply {
    border = UiUtils.EMPTY_BORDER

    val bag = UiUtils.createDefaultGridBag()

    add(AnActionOptionButton(createRunGarbageCollectionRunOption()), bag.nextLine().next())
    add(UiUtils.createContextHelpLabel("Medium performance impact on the JVM."), bag.next().anchor(GridBagConstraints.WEST).overrideLeftInset(UIUtil.DEFAULT_HGAP / 2))

    // Stretch panel horizontally
    add(JPanel(), bag.nextLine().next().coverLine().weightx(1.0).fillCellHorizontally())
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createRunGarbageCollectionRunOption(): AnAction {
    val onSuccess: (String, JvmActionContext) -> Unit = { _, jvmActionContext ->
      NotificationUtils.notifyBalloon("Run Garbage Collection",
                                      "Garbage collection triggered on PID ${jvmActionContext.processNode.process.processID}.",
                                      jvmActionContext.project)
    }
    val commandLine: (JvmActionContext) -> Pair<JTool, List<String>> = {
      Pair(JTool.JCMD, listOf(it.processNode.process.processID.toString(), "GC.run"))
    }
    return ToStringJToolRunOption("Run Garbage Collection", { "Running Garbage Collection" }, commandLine, onSuccess)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}