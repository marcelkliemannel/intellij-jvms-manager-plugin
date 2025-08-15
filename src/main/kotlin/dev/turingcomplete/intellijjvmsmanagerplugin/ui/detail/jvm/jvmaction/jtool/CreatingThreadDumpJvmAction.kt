package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.jtool

import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBCheckBox
import com.intellij.unscramble.AnalyzeStacktraceUtil
import com.intellij.unscramble.ThreadDumpConsoleFactory
import com.intellij.unscramble.ThreadDumpParser
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.AnActionOptionButton
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.TextPopup
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.overrideTopInset
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmAction
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmActionContext
import java.awt.GridBagLayout
import javax.swing.JPanel

class CreatingThreadDumpJvmAction : JvmAction("Thread Dump") {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //

  private lateinit var printAdditionalLocksInformation: JBCheckBox
  private lateinit var printAdditionalThreadsInformation: JBCheckBox

  private val createJToolCommand: (JvmActionContext) -> Pair<JTool, List<String>> = {
    createJToolCommand(it)
  }

  private val toTaskTitleFunction: (JvmActionContext) -> String = {
    "Creating Thread Dump of PID ${it.processNode.process.processID}"
  }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun createComponent(project: Project, parent: Disposable) =
    JPanel(GridBagLayout()).apply {
      border = UiUtils.EMPTY_BORDER

      val bag = UiUtils.createDefaultGridBag()

      printAdditionalLocksInformation = JBCheckBox("Print additional information about locks", true)
      printAdditionalThreadsInformation =
        JBCheckBox("Print additional information about threads", true)
      add(
        printAdditionalLocksInformation,
        bag.nextLine().next().weightx(1.0).fillCellHorizontally(),
      )
      add(
        printAdditionalThreadsInformation,
        bag.nextLine().next().weightx(1.0).fillCellHorizontally(),
      )

      val runButton =
        AnActionOptionButton(
          createOpenResultInThreadDumpAnalyzerRunOption(),
          createOpenResultInPopupRunOption(),
          createToFileJToolRunOption(),
        )
      add(runButton, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP))
    }

  // -- Private Methods ----------------------------------------------------- //

  private fun createOpenResultInThreadDumpAnalyzerRunOption(): AnAction {
    val onSuccess: (String, JvmActionContext) -> Unit = { threadDump, jvmActionContext ->
      val threadStates = ThreadDumpParser.parse(threadDump)
      val project = jvmActionContext.project
      val threadDumpConsoleFactory =
        if (threadStates.size > 1) ThreadDumpConsoleFactory(project, threadStates) else null
      val tabTitle =
        "${jvmActionContext.processNode.smartName} (${jvmActionContext.processNode.process.processID})"
      val icon = AllIcons.Actions.Dump
      AnalyzeStacktraceUtil.addConsole(
        project,
        threadDumpConsoleFactory,
        tabTitle,
        threadDump,
        icon,
      )
    }
    return ToStringJToolRunOption(
      "Run - Show In Thread Dump Analyzer",
      toTaskTitleFunction,
      createJToolCommand,
      onSuccess,
    )
  }

  private fun createOpenResultInPopupRunOption(): AnAction {
    val onSuccess: (String, JvmActionContext) -> Unit = { result, jvmActionContext ->
      val title = "Thread Dump of PID ${jvmActionContext.processNode.process.processID}"
      TextPopup.showCenteredInCurrentWindow(title, result, jvmActionContext.project, false)
    }
    return ToStringJToolRunOption(
      "Run - Show Output",
      toTaskTitleFunction,
      createJToolCommand,
      onSuccess,
    )
  }

  private fun createToFileJToolRunOption(): AnAction {
    return ToFileJToolRunOption(toTaskTitleFunction, "thread-dump_", createJToolCommand)
  }

  private fun createJToolCommand(jvmActionContext: JvmActionContext): Pair<JTool, List<String>> {
    val command = mutableListOf<String>()

    if (printAdditionalLocksInformation.isSelected) {
      command.add("-l")
    }
    if (printAdditionalThreadsInformation.isSelected) {
      command.add("-e")
    }
    command.add(jvmActionContext.processNode.process.processID.toString())

    return Pair(JTool.JSTACK, command)
  }
}
