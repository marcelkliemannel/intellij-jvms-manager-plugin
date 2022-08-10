package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.OSProcessHandler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.StringAppenderProcessAdapter
import org.apache.commons.lang.WordUtils

class RunCommandTask(project: Project,
                     title: String,
                     private val errorMessage: String,
                     private val commandLine: GeneralCommandLine,
                     private val onSuccess: (String) -> Unit,
                     private val onFinished: () -> Unit = {})
  : Task.ConditionalModal(project, title, false, DEAF) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private const val TIMEOUT_MILLIS: Long = 30 * 1000
    private val LOG = Logger.getInstance(RunCommandTask::class.java)
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var output: String = ""

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun run(indicator: ProgressIndicator) {
    LOG.info("Start command: " + commandLine.commandLineString)

    val processHandler = OSProcessHandler(commandLine)
    LOG.assertTrue(!processHandler.isStartNotified)
    val stringAppenderProcessAdapter = StringAppenderProcessAdapter()
    processHandler.addProcessListener(stringAppenderProcessAdapter)
    processHandler.startNotify()

    if (!processHandler.waitFor(TIMEOUT_MILLIS)) {
      throw RuntimeException("Command execution took more than ${TIMEOUT_MILLIS / 1000} seconds.")
    }

    output = stringAppenderProcessAdapter.output()
  }

  override fun onSuccess() {
    this.onSuccess(output)
  }

  override fun onFinished() {
    this.onFinished.invoke()
  }

  override fun onThrowable(error: Throwable) {
    val enhancedErrorMessage = "$errorMessage: ${error.message}"
    LOG.warn(enhancedErrorMessage, error)
    Messages.showErrorDialog(project,
                             "$enhancedErrorMessage\n\n" +
                             "See idea.log for more details.",
                             "${WordUtils.capitalize(title)} Failed")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}