package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.jtool

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Key
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.NotificationUtils.notifyBalloon
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmActionContext
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.outputStream

class ToFileJToolRunOption(
  taskTitle: (JvmActionContext) -> String,
  private val defaultFileNamePrefix: String,
  private val createJToolCommand: (JvmActionContext) -> Pair<JTool, List<String>>,
) : JToolRunOption(OPTION_TITLE, taskTitle, AllIcons.Actions.MenuSaveall) {
  // -- Companion Object ---------------------------------------------------- //

  companion object {
    const val OPTION_TITLE = "Run - Save Output to File"

    private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-SS")
  }

  // -- Properties ---------------------------------------------------------- //

  private var processAdapter: OutputFileProcessAdapter? = null

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun beforeExecution(jvmActionContext: JvmActionContext): Boolean {
    val fileSaverDescriptor = FileSaverDescriptor("Save Action Output As", "")
    val timeStamp = LocalDateTime.now().format(TIMESTAMP_FORMAT)
    val processID = jvmActionContext.processNode.process.processID
    val defaultFilename = "$defaultFileNamePrefix${processID}_$timeStamp.txt"
    return FileChooserFactory.getInstance()
      .createSaveFileDialog(fileSaverDescriptor, jvmActionContext.project)
      .save(defaultFilename)
      ?.file
      ?.toPath()
      ?.let {
        this.processAdapter = OutputFileProcessAdapter(it)
        true
      } ?: false
  }

  override fun createJToolCommand(jvmActionContext: JvmActionContext): Pair<JTool, List<String>> {
    return createJToolCommand.invoke(jvmActionContext)
  }

  override fun getProcessAdapter(): ProcessAdapter? = processAdapter!!

  override fun onSuccess(jvmActionContext: JvmActionContext) {
    val exitCode = processAdapter!!.exitCode()
    val theTaskTitle = taskTitle(jvmActionContext)
    val options =
      arrayOf(
        OpenOutputAction("Open Output File", processAdapter!!.outputFile),
        OpenOutputAction("Open Output Directory", processAdapter!!.outputFile.parent),
      )

    if (exitCode == 0) {
      notifyBalloon(
        theTaskTitle,
        "Output written to file: ${processAdapter!!.outputFile.fileName}",
        jvmActionContext.project,
        NotificationType.INFORMATION,
        *options,
      )
    } else {
      notifyBalloon(
        "$theTaskTitle Failed",
        "Command failed with exit code: $exitCode. See output file for more information.",
        jvmActionContext.project,
        NotificationType.ERROR,
        *options,
      )
    }
  }

  override fun onFinished(jvmActionContext: JvmActionContext) {
    processAdapter!!.close()
    processAdapter = null
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private class OutputFileProcessAdapter(val outputFile: Path) : ProcessAdapter() {

    private val outputStream: OutputStream by lazy { createOutputStream(outputFile) }
    private var exitCode = 0

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
      outputStream.write(event.text.toByteArray())
    }

    override fun processTerminated(event: ProcessEvent) {
      exitCode = event.exitCode
    }

    fun exitCode(): Int = exitCode

    fun close() {
      outputStream.close()
    }

    private fun createOutputStream(outputFile: Path): OutputStream {
      return outputFile.outputStream(
        StandardOpenOption.WRITE,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
      )
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class OpenOutputAction(title: String, val output: Path) : DumbAwareAction(title) {

    override fun actionPerformed(e: AnActionEvent) {
      BrowserUtil.browse(output.toFile())
    }
  }
}
