package dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.jtool

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.ide.BrowserUtil
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.Key
import dev.turingcomplete.intellijjpsplugin.ui.action.jvmaction.JvmActionContext
import dev.turingcomplete.intellijjpsplugin.ui.common.NotificationUtils.notify
import java.io.OutputStream
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.outputStream

class ToFileJToolRunOption(taskTitle: (JvmActionContext) -> String,
                           private val defaultFileNamePrefix: String,
                           private val createJToolCommand: (JvmActionContext) -> Pair<JTool, List<String>>)
  : JToolRunOption("Run - Save to File", taskTitle) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-SS")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var processAdapter: OutputFileProcessAdapter? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun beforeExecution(jvmActionContext: JvmActionContext): Boolean {
    val fileSaverDescriptor = FileSaverDescriptor("Save Action Output As", "")
    val timeStamp = LocalDateTime.now().format(TIMESTAMP_FORMAT)
    val processID = jvmActionContext.processNode.process.processID
    val defaultFilename = "$defaultFileNamePrefix${processID}_$timeStamp.txt"
    return FileChooserFactory.getInstance()
                   .createSaveFileDialog(fileSaverDescriptor, jvmActionContext.project)
                   .save(defaultFilename)?.file?.toPath()
                   ?.let { this.processAdapter = OutputFileProcessAdapter(it); true } ?: false
  }

  override fun createJToolCommand(jvmActionContext: JvmActionContext): Pair<JTool, List<String>> {
    return createJToolCommand.invoke(jvmActionContext)
  }

  override fun getProcessAdapter(): ProcessAdapter? = processAdapter!!

  override fun onSuccess(jvmActionContext: JvmActionContext) {
    notify(taskTitle(jvmActionContext),
           "Output written to file: ${processAdapter!!.outputFile.fileName}",
           jvmActionContext.project,
           NotificationType.INFORMATION,
           OpenOutput("Open Output File", processAdapter!!.outputFile),
           OpenOutput("Open Output Directory", processAdapter!!.outputFile.parent))
  }

  override fun onFinished(jvmActionContext: JvmActionContext) {
    processAdapter!!.close()
    processAdapter = null
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OutputFileProcessAdapter(val outputFile: Path) : ProcessAdapter() {

    private val outputStream: OutputStream by lazy { createOutputStream(outputFile) }

    override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
      outputStream.write(event.text.toByteArray())
    }

    fun close() {
      outputStream.close()
    }

    private fun createOutputStream(outputFile: Path): OutputStream {
      return outputFile.outputStream(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class OpenOutput(title: String, val output: Path) : DumbAwareAction(title) {

    override fun actionPerformed(e: AnActionEvent) {
      BrowserUtil.browse(output.toFile())
    }
  }
}