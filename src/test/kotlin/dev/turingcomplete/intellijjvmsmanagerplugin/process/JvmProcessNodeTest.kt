package dev.turingcomplete.intellijjvmsmanagerplugin.process

import com.sun.tools.attach.VirtualMachineDescriptor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import oshi.software.os.OSProcess

class JvmProcessNodeTest {
  // -- Companion Object ---------------------------------------------------- //
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @ParameterizedTest
  @CsvSource(
    "java -jar foo.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=1044|1044",
    "java -jar foo.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=127.0.0.1:1044|127.0.0.1:1044",
    "java -jar foo.jar -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=::1:1044|::1:1044",
    "java -jar foo.jar -agentlib:jdwp=server=y,suspend=n,address=1044,transport=dt_socket|1044",
    "java -jar foo.jar -agentlib:jdwp=address=1044|1044",
    "java -jar foo.jar -agentlib:jdwp=fooaddress=1044|",
    "java -agentlib:jdwp=server=y,suspend=n,address=1044,transport=dt_socket-jar --add-opens foo=bar foo.jar|1044",
    delimiter = '|',
  )
  fun testDebugAgentPortExtraction(commandLine: String, expectedAddress: String?) {
    val testOsProcess = mock(OSProcess::class.java)
    `when`(testOsProcess.commandLine).thenReturn(commandLine)

    val jvmProcessNode = JvmProcessNode(testOsProcess, mock(VirtualMachineDescriptor::class.java))

    assertEquals(expectedAddress, jvmProcessNode.debugAgentAddress)
  }

  @ParameterizedTest
  @CsvSource(
    "java -jar foo.jar -javaagent:agent.jar|agent.jar|",
    "java -jar foo.jar -javaagent:agent.jar=|agent.jar|",
    "java -jar foo.jar -javaagent:agent.jar=myOptions|agent.jar|myOptions",
    "java -jar foo.jar -javaagent:agent.jar=myOptions=myOptions|agent.jar|myOptions=myOptions",
    "java -jar foo.jar -javaagent:agent.jar=myOptions:myOptions|agent.jar|myOptions:myOptions",
    "java -jar foo.jar -javaagent:C:/Users/John/agent.jar=myOptions|C:/Users/John/agent.jar|myOptions",
    "java -jar foo.jar -javaagent:/Users/John/agent.jar=myOptions|/Users/John/agent.jar|myOptions",
    "java -javaagent:agent.jar=myOptions --add-opens foo=bar foo.jar|agent.jar|myOptions",
    delimiter = '|',
  )
  fun testJavaAgentExtraction(commandLine: String, agentPath: String, options: String?) {
    val testOsProcess = mock(OSProcess::class.java)
    `when`(testOsProcess.commandLine).thenReturn(commandLine)

    val jvmProcessNode = JvmProcessNode(testOsProcess, mock(VirtualMachineDescriptor::class.java))
    assertEquals(mapOf(Pair(agentPath, options)), jvmProcessNode.javaAgents)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
