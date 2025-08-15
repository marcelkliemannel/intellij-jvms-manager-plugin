package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.jtool

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.AnActionOptionButton
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.TextPopup
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.overrideTopInset
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmAction
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmActionContext
import java.awt.GridBagLayout
import java.lang.reflect.Method
import javax.swing.JPanel

class CreatingThreadDumpJvmAction : JvmAction("Thread Dump") {
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

        printAdditionalLocksInformation =
            JBCheckBox("Print additional information about locks", true)
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
      // Parser and unscramble classes have been made internal in a recent
      // IntelliJ version.
      try {
        val parserClass = Class.forName(PARSER_CLASS)
        val unscrambleClass = Class.forName(UNSCRAMBLE_CLASS)

        val parseMethod: Method =
            parserClass.getMethod(PARSER_CLASS_PARSE_METHOD, String::class.java)
        val threadStates = parseMethod.invoke(null, threadDump) as MutableList<*>?

        val addConsoleMethod: Method =
            unscrambleClass.getMethod(
                UNSCRAMBLE_CLASS_ADD_CONSOLE_METHOD,
                Project::class.java,
                MutableList::class.java,
                String::class.java)

        addConsoleMethod.invoke(null, jvmActionContext.project, threadStates, threadDump)
      } catch (e: Exception) {
        LOG.warn("Failed to open IntelliJ's Thread Dump Analyzer", e)
        Messages.showErrorDialog(
            jvmActionContext.project,
            "Failed to open IntelliJ's Thread Dump Analyzer.\n\nSee idea.log for more details.",
            "Run - Show In Thread Dump Analyzer",
        )
      }
    }

    val available: () -> Boolean = {
      try {
        val parserClass = Class.forName(PARSER_CLASS)
        val unscrambleClass = Class.forName(UNSCRAMBLE_CLASS)

        parserClass.getMethod(PARSER_CLASS_PARSE_METHOD, String::class.java)
        unscrambleClass.getMethod(
            UNSCRAMBLE_CLASS_ADD_CONSOLE_METHOD,
            Project::class.java,
            MutableList::class.java,
            String::class.java)
        true
      } catch (_: ClassNotFoundException) {
        false
      } catch (_: NoSuchMethodException) {
        false
      }
    }

    return ToStringJToolRunOption(
        "Run - Show In Thread Dump Analyzer",
        toTaskTitleFunction,
        createJToolCommand,
        onSuccess,
        available = available)
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

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val LOG = Logger.getInstance(CreatingThreadDumpJvmAction::class.java)

    private const val PARSER_CLASS = "com.intellij.threadDumpParser.ThreadDumpParser"
    private const val PARSER_CLASS_PARSE_METHOD = "parse"

    private const val UNSCRAMBLE_CLASS = "com.intellij.unscramble.UnscrambleUtils"
    private const val UNSCRAMBLE_CLASS_ADD_CONSOLE_METHOD = "addConsole"
  }
}
