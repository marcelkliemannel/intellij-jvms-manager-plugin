package dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.jvmaction.jtool

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Key
import dev.turingcomplete.intellijjpsplugin.ui.common.NotificationUtils
import dev.turingcomplete.intellijjpsplugin.ui.common.TextPopup
import dev.turingcomplete.intellijjpsplugin.ui.detail.jvm.jvmaction.JvmActionContext

class ToStringJToolRunOption(optionTitle: String,
                             taskTitle: (JvmActionContext) -> String,
                             private val createJToolCommand: (JvmActionContext) -> Pair<JTool, List<String>>,
                             private val onSuccess: (String, JvmActionContext) -> Unit,
                             private val mayProduceHighMemoryResult: () -> Boolean = { false })
  : JToolRunOption(optionTitle, taskTitle) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var processAdapter: StringAppenderProcessAdapter? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun beforeExecution(jvmActionContext: JvmActionContext): Boolean {
    if (mayProduceHighMemoryResult()) {
      val shouldContinue = Messages.showYesNoDialog(jvmActionContext.project,
                                                    "The output of the action may not be processable in-memory " +
                                                    "and thus result in an \"Out of Memory\" error in IntelliJ. Consider using " +
                                                    "the \"Save to File\" option instead.\n\nShould the action be executed?",
                                                    taskTitle(jvmActionContext),
                                                    null)
      if (shouldContinue != Messages.YES) {
        return false
      }
    }

    processAdapter = StringAppenderProcessAdapter()

    return true
  }

  override fun createJToolCommand(jvmActionContext: JvmActionContext): Pair<JTool, List<String>> {
    return createJToolCommand.invoke(jvmActionContext)
  }

  override fun getProcessAdapter(): ProcessAdapter? = processAdapter!!

  override fun onSuccess(jvmActionContext: JvmActionContext) {
    val exitCode = processAdapter!!.exitCode()
    val output = processAdapter!!.output()

    if (exitCode == 0) {
      onSuccess(output, jvmActionContext)
    }
    else {
      val theTaskTitle = taskTitle(jvmActionContext)
      NotificationUtils.notifyBalloon("$theTaskTitle Failed",
                                      "Command failed with exit code: $exitCode.",
                                      jvmActionContext.project,
                                      NotificationType.ERROR,
                                      ShowProcessActionOutput(theTaskTitle, output, jvmActionContext))
    }
  }

  override fun onFinished(jvmActionContext: JvmActionContext) {
    processAdapter = null
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class StringAppenderProcessAdapter : ProcessAdapter() {

    private val output = StringBuilder()
    private var exitCode = 0

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
      output.append(event.text)
    }

    override fun processTerminated(event: ProcessEvent) {
      exitCode = event.exitCode
    }

    fun output(): String = output.toString()

    fun exitCode(): Int = exitCode
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ShowProcessActionOutput(val taskTitle: String,
                                        val output: String,
                                        val jvmActionContext: JvmActionContext)
    : DumbAwareAction("Show Process Output") {

    override fun actionPerformed(e: AnActionEvent) {
      val title = "Output of '$taskTitle'"
      TextPopup.showCenteredInCurrentWindow(title, output, jvmActionContext.project, false)
    }
  }
}