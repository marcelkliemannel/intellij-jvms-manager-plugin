package dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.jtool

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.Messages
import dev.turingcomplete.intellijjpsplugin.ui.JavaProcessesPanel
import dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.JvmActionContext
import dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.JvmActionException
import java.nio.file.Path
import javax.swing.Icon

abstract class JToolRunOption(optionTitle: String,
                              val taskTitle: (JvmActionContext) -> String,
                              val icon : Icon = AllIcons.RunConfigurations.TestState.Run)
  : DumbAwareAction(optionTitle, null, icon) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  final override fun actionPerformed(e: AnActionEvent) {
    val jvmActionContext = JvmActionContext.DATA_KEY.getData(e.dataContext)
                           ?: throw IllegalStateException("Data context is missing required data key '${JvmActionContext.DATA_KEY.name}'.")

    val shouldContinue = beforeExecution(jvmActionContext)
    if (!shouldContinue) {
      return
    }

    JToolActionTask(jvmActionContext, this, taskTitle(jvmActionContext)).queue()
  }

  open fun getProcessAdapter(): ProcessAdapter? = null

  abstract fun createJToolCommand(jvmActionContext: JvmActionContext): Pair<JTool, List<String>>

  open fun beforeExecution(jvmActionContext: JvmActionContext): Boolean = true

  open fun onSuccess(jvmActionContext: JvmActionContext) {
  }

  open fun onFinished(jvmActionContext: JvmActionContext) {
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class JToolActionTask(private val jvmActionContext: JvmActionContext,
                                private val jToolRunOption: JToolRunOption,
                                private val taskTitle: String)
    : Task.ConditionalModal(jvmActionContext.project, taskTitle, false, DEAF) {

    companion object {
      private val LOG = Logger.getInstance(JavaProcessesPanel::class.java)
      private const val TIMEOUT_MILLIS: Long = 30 * 1000
    }

    override fun run(indicator: ProgressIndicator) {
      val jdk = jvmActionContext.jdk() ?: throw IllegalStateException("snh: No JDK selected. Task should not be accessible if not JDK was selected.")
      val jdkHomePath = jdk.homePath ?: throw JvmActionException("The selected JDK does not have a home path configured.")

      val (jTool, arguments) = jToolRunOption.createJToolCommand(jvmActionContext)
      val commandLine = GeneralCommandLine(jTool.executable(Path.of(jdkHomePath)).toString())
      arguments.forEach {
        commandLine.addParameter(it)
      }

      val processHandler = OSProcessHandler(commandLine)
      LOG.assertTrue(!processHandler.isStartNotified)

      jToolRunOption.getProcessAdapter()?.let { processHandler.addProcessListener(it) }
      processHandler.startNotify()

      if (!processHandler.waitFor(TIMEOUT_MILLIS)) {
        throw JvmActionException("Command execution took more than ${TIMEOUT_MILLIS / 1000} seconds.")
      }
    }

    override fun onThrowable(error: Throwable) {
      val errorMessage = "Failed to run JVM Action '$taskTitle': ${error.message}"
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(project, "$errorMessage\n\nSee idea.log for more details.", "$taskTitle failed")
    }

    override fun onSuccess() {
      jToolRunOption.onSuccess(jvmActionContext)
    }

    override fun onFinished() {
      jToolRunOption.onFinished(jvmActionContext)
    }
  }
}