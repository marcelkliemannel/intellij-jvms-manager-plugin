package dev.turingcomplete.intellijjvmsmanagerplugin.process

import com.intellij.openapi.application.ApplicationManager
import com.sun.tools.attach.VirtualMachine
import com.sun.tools.attach.VirtualMachineDescriptor
import oshi.software.os.OSProcess
import java.util.*

class JvmProcessNode(process: OSProcess, private val vmDescriptor: VirtualMachineDescriptor) : ProcessNode(process) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val entryPoint: EntryPoint? by lazy { determineEntryPoint() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun determineProcessType(): ProcessType = ProcessType.determine(process, vmDescriptor)

  override fun determineSmartName(): String {
    return entryPoint?.fullName ?: super.determineSmartName()
  }

  fun collectSystemProperties(): Properties {
    assert(!ApplicationManager.getApplication().isDispatchThread)

    return runOnVirtualMachine { it.systemProperties }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun <T> runOnVirtualMachine(action: (VirtualMachine) -> T): T {
    val virtualMachine = vmDescriptor.provider().attachVirtualMachine(vmDescriptor)
    try {
      return action(virtualMachine)
    }
    finally {
      virtualMachine.detach()
    }
  }

  private fun determineEntryPoint(): EntryPoint? {
    // We keep it a bit simple at this point. IntelliJ usually runs on a JRE
    // based on Open JDK. The Open JDK provides the running Java process by
    // `sun.tools.attach.HotSpotAttachProvider.listVirtualMachines` which we
    // are indirectly using to get the `VirtualMachineDescriptor`. The current
    // Open JDK implementation uses for the `vmDisplayName`:
    // - The process PID, if the Java process is not a HotSpot JVM;
    // - the main class plus arguments;
    // - or the entry JaR plus arguments.
    // For performance reasons, we will only cover these three cases.
    // If we want to be independently of the Open JDK implementation, we could
    // go through the command line to find the `-jar` or main class with a
    // regex. But what would have a higher performance impact.
    val vmDisplayName = vmDescriptor.displayName() ?: return null

    if (vmDisplayName == process.processID.toString()) {
      return null
    }

    val entryPointName = vmDisplayName.substringBefore(" ")
    return if (entryPointName.endsWith(".jar")) {
      val jarFileName = entryPointName.substringAfter("/")
      EntryPoint("Entry Jar", jarFileName, entryPointName)
    }
    else if (entryPointName.isNotBlank()) {
      val simpleName = entryPointName.substringAfterLast(".")
      EntryPoint("Main Class", simpleName, entryPointName)
    }
    else {
      null
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class EntryPoint(val typeTitle: String, val shortName: String, val fullName: String)
}