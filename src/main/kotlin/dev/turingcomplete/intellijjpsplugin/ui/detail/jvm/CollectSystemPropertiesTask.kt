package dev.turingcomplete.intellijjpsplugin.ui.detail.jvm

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import dev.turingcomplete.intellijjpsplugin.process.JvmProcessNode
import java.util.*

class CollectSystemPropertiesTask(project: Project,
                                  private val processNode: JvmProcessNode,
                                  private val onSuccess: (Properties) -> Unit,
                                  private val onFinished: () -> Unit)
  : Task.ConditionalModal(project, "Collecting system properties", false, DEAF) {

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val LOG = Logger.getInstance(JvmActionsTab::class.java)
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var systemProperties : Properties

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun run(indicator: ProgressIndicator) {
    systemProperties = processNode.collectSystemProperties()
  }

  override fun onSuccess() {
    this.onSuccess(systemProperties)
  }

  override fun onFinished() {
    this.onFinished.invoke()
  }

  override fun onThrowable(error: Throwable) {
    val errorMessage = "Failed to collect system properties of PID ${processNode.process.processID}: ${error.message}"
    LOG.warn(errorMessage, error)
    Messages.showErrorDialog(project, "$errorMessage\nSee idea.log for more details.", "Collecting System Properties Failed")
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}