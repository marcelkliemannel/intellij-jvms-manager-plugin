package dev.turingcomplete.intellijjpsplugin.ui.detail.jvmaction

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ScriptRunnerUtil
import com.intellij.execution.process.ScriptRunnerUtil.getProcessOutput
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijjpsplugin.process.OshiUtils
import dev.turingcomplete.intellijjpsplugin.ui.JavaProcessesPanel
import dev.turingcomplete.intellijjpsplugin.ui.common.UiUtils
import dev.turingcomplete.intellijjpsplugin.ui.common.overrideTopInset
import oshi.PlatformEnum
import java.awt.GridBagLayout
import java.awt.event.ActionEvent
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.AbstractAction
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

abstract class JvmAction(val title: String, protected val taskTitle: String, protected val jTool: JTool) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    @JvmStatic
    protected val PID_PLACEHOLDER = "%PID%"

    val EP: ExtensionPointName<JvmAction> = ExtensionPointName.create("dev.turingcomplete.intellijjpsplugin.jvmAction")

    private val LOG = Logger.getInstance(JavaProcessesPanel::class.java)
    private const val TIMEOUT_MILLIS : Long = 30 * 1000
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private lateinit var runButton: JButton

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  fun createComponent(jvmActionContext: JvmActionContext) = JPanel(GridBagLayout()).apply {
    border = UiUtils.EMPTY_BORDER

    val bag = UiUtils.createDefaultGridBag()

    createConfigurationComponent()?.let {
      add(it, bag.nextLine().next().weightx(1.0).fillCellHorizontally())
    }

    val runAction = object : AbstractAction("Run", AllIcons.RunConfigurations.TestState.Run) {
      override fun actionPerformed(e: ActionEvent?) {
        RunJvmActionTask(jvmActionContext, taskTitle, jTool, createActionParameters()) { onSuccess(it, jvmActionContext) }
                .queue()
      }
    }
    runButton = JButton(runAction)
    add(runButton, bag.nextLine().next().overrideTopInset(UIUtil.DEFAULT_HGAP))
  }

  protected abstract fun onSuccess(result: String?, jvmActionContext: JvmActionContext)

  protected abstract fun createConfigurationComponent(): JComponent?

  protected abstract fun createActionParameters(): List<String>

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  enum class JTool(private val unixExecutable: String, private val windowsExecutable: String) {
    JSTACK("bin/jstack", "bin/jstack.exe");

    fun executable(): String {
      return when (OshiUtils.CURRENT_PLATFORM) {
        PlatformEnum.WINDOWS -> windowsExecutable
        else -> unixExecutable
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class RunJvmActionTask(val jvmActionContext: JvmActionContext,
                                 val taskTitle: String,
                                 val jTool: JTool,
                                 val actionParameters: List<String>,
                                 val onSuccess: (String?) -> Unit)
    : Task.ConditionalModal(jvmActionContext.project, taskTitle, true, DEAF) {

    private var result: String? = null

    override fun run(indicator: ProgressIndicator) {
      val commandLine = GeneralCommandLine(getJToolPath().toString())
      val processID = jvmActionContext.processNode.process.processID
      actionParameters.map { it.replace(PID_PLACEHOLDER, processID.toString()) }.forEach { commandLine.addParameter(it) }
      result = getProcessOutput(commandLine, ScriptRunnerUtil.STDOUT_OR_STDERR_OUTPUT_KEY_FILTER, TIMEOUT_MILLIS)
    }

    override fun onSuccess() {
      onSuccess(result)
    }

    override fun onThrowable(error: Throwable) {
      val errorMessage = "Failed to run JVM Action '$title': ${error.message}"
      LOG.warn(errorMessage, error)
      Messages.showErrorDialog(project, "$errorMessage\nSee idea.log for more details.", "$taskTitle Failed")
    }

    private fun getJToolPath(): Path {
      val jdk = jvmActionContext.jdk() ?: throw IllegalStateException("snh: No JDK selected. Run task should not be accessible if not JDK was selected.")
      val jdkHomePath = jdk.homePath ?: throw JvmActionException("The selected JDK does not have a home path configured.")

      val jToolPath = Path.of(jdkHomePath).resolve(jTool.executable())
      if (!Files.exists(jToolPath)) {
        throw JvmActionException("Can't find executable in the selected JDK: $jToolPath")
      }

      return jToolPath
    }
  }
}