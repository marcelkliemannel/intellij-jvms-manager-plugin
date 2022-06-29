package dev.turingcomplete.intellijjpsplugin.process

import com.intellij.icons.AllIcons
import com.intellij.ui.IconManager
import com.sun.tools.attach.VirtualMachineDescriptor
import dev.turingcomplete.intellijjpsplugin.ui.common.CommonIcons
import oshi.software.os.OSProcess
import javax.swing.Icon

enum class ProcessType(val description: String?, private val loadIcon: (() -> Icon), val url: String? = null) {
  // -- Values ------------------------------------------------------------------------------------------------------ //

  INTELLIJ_IDEA("IntelliJ IDEA", "intellij.svg"),
  INTELLIJ_IDEA_FSNOTIFIER("IntelliJ IDEA File Watcher", "intellij.svg", "https://github.com/int128/idea-fsnotifier-wsl"),
  GRADLE_DAEMON("Gradle Daemon", "gradle.svg", "https://docs.gradle.org/current/userguide/gradle_daemon.html"),
  KOTLIN_COMPILE_DAEMON("Kotlin Compile Daemon", "kotlin.svg"),
  MAVEN_WRAPPER("Maven Wrapper", "maven.svg", "https://maven.apache.org/wrapper/"),
  GIT("Git", "git.svg", "https://git-scm.com/docs"),
  JAVAC("Java Compiler", { CommonIcons.JAVA }, "https://docs.oracle.com/en/java/javase/17/docs/specs/man/javac.html"),
  JAVA("Java Virtual Machine", { CommonIcons.JAVA }, "https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html"),
  JCEF("Java Chromium Embedded Framework", { CommonIcons.JAVA }, "https://github.com/JetBrains/jcef"),
  UNKNOWN(null, { AllIcons.Debugger.Console });

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val processNameToType = mapOf(Pair("javac", JAVAC), Pair("fsnotifier", INTELLIJ_IDEA_FSNOTIFIER), Pair("jcef", JCEF),
                                          Pair("git", GIT), Pair("idea", INTELLIJ_IDEA), Pair("idea64", INTELLIJ_IDEA))

    fun determine(process: OSProcess, vmDescriptor: VirtualMachineDescriptor?): ProcessType {
      return processNameToType[process.name] ?: when {
        process.commandLine?.contains("com.intellij.idea.Main") == true -> INTELLIJ_IDEA
        process.commandLine?.contains("org.gradle.launcher.daemon.bootstrap.GradleDaemon") == true -> GRADLE_DAEMON
        process.commandLine?.contains("org.jetbrains.kotlin.daemon.KotlinCompileDaemon") == true -> KOTLIN_COMPILE_DAEMON
        process.commandLine?.contains("org.apache.maven.wrapper.MavenWrapperMain") == true -> MAVEN_WRAPPER
        process.name == "java" || vmDescriptor != null -> JAVA
        else -> UNKNOWN
      }
    }
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //

  val icon: Icon by lazy { loadIcon() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  constructor(description: String, iconFileName: String?, url: String? = null)
          : this(description, { IconManager.getInstance().getIcon("dev/turingcomplete/intellijjpsplugin/icons/${iconFileName}", ProcessType::class.java) }, url)

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}