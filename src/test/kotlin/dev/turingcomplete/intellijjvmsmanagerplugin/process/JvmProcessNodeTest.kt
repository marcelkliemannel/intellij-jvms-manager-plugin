package dev.turingcomplete.intellijjvmsmanagerplugin.process

import com.sun.tools.attach.VirtualMachineDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import oshi.software.os.OSProcess

class JvmProcessNodeTest {
  // -- Companion Object -------------------------------------------------------------------------------------------- //
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  // address=127.0.0.1:64440
  @ParameterizedTest
  @CsvSource("java -jar foo.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044|1044",
             "java -jar foo.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:1044|127.0.0.1:1044",
             "java -jar foo.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=::1:1044|::1:1044",
             "java -jar foo.jar -agentlib:jdwp=server=y,suspend=n,address=1044,transport=dt_socket|1044",
             "java -jar foo.jar -agentlib:jdwp=address=1044|1044",
             "java -jar foo.jar -agentlib:jdwp=fooaddress=1044|",
             "java -agentlib:jdwp=server=y,suspend=n,address=1044,transport=dt_socket-jar --add-opens foo=bar foo.jar|1044",
            delimiter = '|')
  fun testDebugAgentPortExtraction(commandLine: String, expectedAddress: String?) {
    val testOsProcess = mock(OSProcess::class.java)
    `when`(testOsProcess.commandLine).thenReturn(commandLine)

    val jvmProcessNode = JvmProcessNode(testOsProcess, mock(VirtualMachineDescriptor::class.java))

    assertEquals(expectedAddress, jvmProcessNode.debugAgentAddress)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
}