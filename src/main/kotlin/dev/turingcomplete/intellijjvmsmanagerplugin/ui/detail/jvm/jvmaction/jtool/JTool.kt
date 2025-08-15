package dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.jtool

import dev.turingcomplete.intellijjvmsmanagerplugin.process.OshiUtils
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.detail.jvm.jvmaction.JvmActionException
import java.nio.file.Files
import java.nio.file.Path
import oshi.PlatformEnum

enum class JTool(private val executableName: String) {
  // -- Values -------------------------------------------------------------- //

  JSTACK("jstack"),
  JCONSOLE("jconsole"),
  JCMD("jcmd");

  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun executable(jdkHomePath: Path): Path {
    val extension = if (OshiUtils.CURRENT_PLATFORM == PlatformEnum.WINDOWS) ".exe" else ""
    val jToolPath = jdkHomePath.resolve("bin").resolve(executableName + extension)
    if (!Files.exists(jToolPath)) {
      throw JvmActionException("Can't find executable in the selected JDK: $jToolPath")
    }

    return jToolPath
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
