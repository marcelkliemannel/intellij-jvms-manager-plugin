package dev.turingcomplete.intellijjpsplugin.jps

import com.intellij.icons.AllIcons
import com.intellij.ui.IconManager
import com.sun.tools.attach.VirtualMachineDescriptor
import oshi.software.os.OSProcess
import javax.swing.Icon

enum class ProcessType(val description: String?, private val loadIcon: (() -> Icon), url: String? = null) {
  // -- Values ------------------------------------------------------------------------------------------------------ //

  INTELLIJ_IDEA("IntelliJ IDEA", "intellij.svg"),
  INTELLIJ_IDEA_FSNOTIFIER("IntelliJ IDEA File Watcher", "intellij.svg", "https://github.com/int128/idea-fsnotifier-wsl"),
  GRADLE_DAEMON("Gradle Daemon", "gradle.svg"),
  KOTLIN_COMPILE_DAEMON("Kotlin Compile Daemon", "kotlin.svg"),
  MAVEN_WRAPPER("Maven Wrapper", "maven.svg"),
  JAVAC("Java Compiler", "java.svg"),
  JAVA("Java Virtual Machine", "java.svg"),
  JCEF("Java Chromium Embedded Framework", "java.svg", "https://github.com/JetBrains/jcef"),
  UNKNOWN(null, { AllIcons.Debugger.Console });

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    fun determine(process: OSProcess, vmDescriptor: VirtualMachineDescriptor?): ProcessType {
      return when {
        process.name == "javac" -> JAVAC
        process.name == "fsnotifier" -> INTELLIJ_IDEA_FSNOTIFIER
        process.name == "jcef" -> JCEF
        process.name == "idea" || process.commandLine?.contains("com.intellij.idea.Main") == true -> INTELLIJ_IDEA
        process.commandLine?.contains("org.gradle.launcher.daemon.bootstrap.GradleDaemon") == true -> GRADLE_DAEMON
        process.commandLine?.contains("org.jetbrains.kotlin.daemon.KotlinCompileDaemon") == true -> KOTLIN_COMPILE_DAEMON
        process.commandLine?.contains("org.apache.maven.wrapper.MavenWrapperMain") == true -> MAVEN_WRAPPER
        process.name == "java" -> JAVA
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