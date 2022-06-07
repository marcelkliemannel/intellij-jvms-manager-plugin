package dev.turingcomplete.intellijjpsplugin.process

import oshi.SystemInfo
import oshi.software.os.OperatingSystem

object OshiUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val OPERATION_SYSTEM: OperatingSystem by lazy { SystemInfo().operatingSystem }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}