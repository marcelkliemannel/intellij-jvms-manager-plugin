package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.jtool

import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmActionContext

class NotWaitingJToolRunOption(optionTitle: String,
                               taskTitle: (JvmActionContext) -> String,
                               private val createJToolCommand: (JvmActionContext) -> Pair<JTool, List<String>>)
  : JToolRunOption(optionTitle, taskTitle) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun createJToolCommand(jvmActionContext: JvmActionContext): Pair<JTool, List<String>> {
    return createJToolCommand.invoke(jvmActionContext)
  }

  override fun waitForTermination(): Boolean = false

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}