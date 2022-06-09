package dev.turingcomplete.intellijjpsplugin.process

import oshi.PlatformEnum
import oshi.SystemInfo
import oshi.software.os.OperatingSystem

object OshiUtils {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  val OPERATION_SYSTEM: OperatingSystem by lazy { SystemInfo().operatingSystem }
  val CURRENT_PLATFORM: PlatformEnum by lazy { SystemInfo.getCurrentPlatform() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}