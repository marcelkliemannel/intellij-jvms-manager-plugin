package dev.turingcomplete.intellijjvmsmanagerplugin.ui.common

import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.openapi.util.Key

class StringAppenderProcessAdapter : ProcessAdapter() {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //

  private val output = StringBuilder()
  private var exitCode = 0

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
    output.append(event.text)
  }

  override fun processTerminated(event: ProcessEvent) {
    exitCode = event.exitCode
  }

  fun output(): String = output.toString()

  fun exitCode(): Int = exitCode

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
