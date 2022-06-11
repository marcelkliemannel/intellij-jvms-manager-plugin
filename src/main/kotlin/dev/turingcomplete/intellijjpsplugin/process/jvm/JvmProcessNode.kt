package dev.turingcomplete.intellijjpsplugin.process.jvm

import com.sun.tools.attach.VirtualMachineDescriptor
import dev.turingcomplete.intellijjpsplugin.process.ProcessNode
import dev.turingcomplete.intellijjpsplugin.process.ProcessType
import oshi.software.os.OSProcess

class JvmProcessNode(process: OSProcess, private val vmDescriptor: VirtualMachineDescriptor) : ProcessNode(process) {
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    private val MAIN_CLASS_REGEX = Regex("^(?<mainClass>\\S+).*$")
  }

  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun determineProcessType(): ProcessType = ProcessType.determine(process, vmDescriptor)

  override fun determineSmartName() : String {
    val vmDisplayName = vmDescriptor.displayName()

    if (vmDisplayName.isNotBlank()) {
      val mainClassMatch = MAIN_CLASS_REGEX.matchEntire(vmDisplayName)
      if (mainClassMatch != null) {
        // Use main class (the `vmDisplayName` also contains the main args)
        return mainClassMatch.groups["mainClass"]!!.value
      }
      else if (vmDisplayName.endsWith(".jar")) {
        // Use JAR file name
        return vmDisplayName.substringAfter("/")
      }
    }

    return super.determineSmartName()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}