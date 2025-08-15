package dev.turingcomplete.intellijjvmsmanagerplugin.process

import com.intellij.icons.AllIcons
import com.sun.tools.attach.VirtualMachineDescriptor
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.CommonIcons
import dev.turingcomplete.intellijjvmsmanagerplugin.ui.common.UiUtils
import javax.swing.Icon
import oshi.software.os.OSProcess

enum class ProcessType(
  val description: String?,
  private val loadIcon: (() -> Icon),
  val url: String? = null,
) {
  // -- Values -------------------------------------------------------------- //

  INTELLIJ_IDEA("IntelliJ IDEA", "intellij.svg"),
  GRADLE_DAEMON(
    "Gradle Daemon",
    "gradle.svg",
    "https://docs.gradle.org/current/userguide/gradle_daemon.html",
  ),
  GRADLE_WORKER(
    "Gradle Worker",
    "gradle.svg",
    "https://docs.gradle.org/current/userguide/worker_api.html",
  ),
  GRADLE_WRAPPER(
    "Gradle Wrapper",
    "gradle.svg",
    "https://docs.gradle.org/current/userguide/gradle_wrapper.html",
  ),
  KOTLIN_COMPILE_DAEMON("Kotlin Compile Daemon", "kotlin.svg"),
  MAVEN_WRAPPER("Maven Wrapper", "maven.svg", "https://maven.apache.org/wrapper/"),
  GIT("Git", "git.svg", "https://git-scm.com/docs"),
  JAVAC(
    "Java Compiler",
    { CommonIcons.JVM },
    "https://docs.oracle.com/en/java/javase/17/docs/specs/man/javac.html",
  ),
  JAVA(
    "Java Virtual Machine",
    { CommonIcons.JVM },
    "https://docs.oracle.com/en/java/javase/17/docs/specs/man/java.html",
  ),
  JCEF(
    "Java Chromium Embedded Framework",
    { CommonIcons.JVM },
    "https://github.com/JetBrains/jcef",
  ),
  UNKNOWN(null, { AllIcons.Debugger.Console });

  // -- Companion Object ---------------------------------------------------- //

  companion object {
    private val processNameToType =
      mapOf(
        Pair("javac", JAVAC),
        Pair("fsnotifier", INTELLIJ_IDEA),
        Pair("jcef", JCEF),
        Pair("git", GIT),
        Pair("idea", INTELLIJ_IDEA),
        Pair("idea64", INTELLIJ_IDEA),
      )

    fun determine(process: OSProcess, vmDescriptor: VirtualMachineDescriptor?): ProcessType {
      val vmDisplayName = vmDescriptor?.displayName()
      return processNameToType[process.name]
        ?: when {
          vmDisplayName?.startsWith("com.intellij") == true -> INTELLIJ_IDEA
          vmDisplayName?.startsWith("org.jetbrains.kotlin.daemon.KotlinCompileDaemon") == true ->
            KOTLIN_COMPILE_DAEMON
          vmDisplayName?.startsWith("org.jetbrains") == true -> INTELLIJ_IDEA
          vmDisplayName?.startsWith("org.gradle.launcher.daemon.bootstrap.GradleDaemon") == true ->
            GRADLE_DAEMON
          vmDisplayName?.startsWith("worker.org.gradle.process.internal.worker.GradleWorkerMain") ==
            true -> GRADLE_WORKER
          vmDisplayName?.startsWith("org.gradle.wrapper.GradleWrapperMain") == true ->
            GRADLE_WRAPPER
          vmDisplayName?.startsWith("org.apache.maven.wrapper.MavenWrapperMain") == true ->
            MAVEN_WRAPPER
          process.name == "java" || vmDescriptor != null -> JAVA
          else -> UNKNOWN
        }
    }
  }

  // -- Properties ---------------------------------------------------------- //

  val icon: Icon by lazy { loadIcon() }

  // -- Initialization ------------------------------------------------------ //

  constructor(
    description: String,
    iconFileName: String,
    url: String? = null,
  ) : this(description, { UiUtils.loadPluginIcon(iconFileName) }, url)

  // -- Exported Methods ---------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
