package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.jtool

import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.api.Test

class CreatingThreadDumpJvmActionTest {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @Test
  fun `Internal IntelliJ classes and methods required for the action is available`() {
    assertThatCode {
      val parserClass = Class.forName("com.intellij.threadDumpParser.ThreadDumpParser")
      parserClass.getMethod("parse", String::class.java)

      val unscrambleClass = Class.forName("com.intellij.unscramble.UnscrambleUtils")
      unscrambleClass.getMethod(
        "addConsole",
        com.intellij.openapi.project.Project::class.java,
        MutableList::class.java,
        String::class.java
      )
    }.doesNotThrowAnyException()
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
