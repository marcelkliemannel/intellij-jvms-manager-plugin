import org.jetbrains.changelog.Changelog
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  java
  kotlin("jvm") version "1.7.10"
  id("org.jetbrains.intellij") version "1.17.4"
  id("org.jetbrains.changelog") version "2.2.1"
}

group = properties("pluginGroup")
version = properties("pluginVersion")

repositories {
  mavenLocal()
  mavenCentral()
}

dependencies {
  implementation("com.github.oshi:oshi-core:6.6.1") {
    exclude(group = "org.slf4j", module = "slf4j-api")
    exclude(group = "net.java.dev.jna", module = "jna")
    exclude(group = "net.java.dev.jna", module = "jna-platform")
  }

  testImplementation("org.mockito:mockito-core:5.12.0")
  val jUnitVersion = "5.10.3"
  testImplementation("org.junit.jupiter:junit-jupiter-params:$jUnitVersion")
  testImplementation("org.junit.jupiter:junit-jupiter-api:$jUnitVersion")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jUnitVersion")
  implementation(kotlin("stdlib-jdk8"))
}

tasks.withType<Test> {
  useJUnitPlatform()
}

intellij {
  version.set(properties("platformVersion"))
  type.set(properties("platformType"))
  downloadSources.set(properties("platformDownloadSources").toBoolean())
  updateSinceUntilBuild.set(true)
  plugins.set(properties("platformPlugins").split(',').map(String::trim).filter(String::isNotEmpty))
}

changelog {
  val projectVersion = project.version as String
  version.set(projectVersion)
  header.set("[$projectVersion] - ${org.jetbrains.changelog.date()}")
  groups.set(listOf("Added", "Changed", "Removed", "Fixed"))
}

tasks {
  patchPluginXml {
    version.set(properties("pluginVersion"))
    sinceBuild.set(properties("pluginSinceBuild"))
    untilBuild.set(properties("pluginUntilBuild"))
    changeNotes.set(provider { changelog.renderItem(changelog.get(project.version as String), Changelog.OutputType.HTML) })
  }

  runPluginVerifier {
    ideVersions.set(properties("pluginVerifierIdeVersions").split(",").map(String::trim).filter(String::isNotEmpty))
  }

  publishPlugin {
    dependsOn("patchChangelog")
    token.set(project.provider { properties("jetbrains.marketplace.token") })
    channels.set(listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first()))
  }

  signPlugin {
    val jetbrainsDir = File(System.getProperty("user.home"), ".jetbrains")
    certificateChain.set(project.provider { File(jetbrainsDir, "plugin-sign-chain.crt").readText() })
    privateKey.set(project.provider { File(jetbrainsDir, "plugin-sign-private-key.pem").readText() })

    password.set(project.provider { properties("jetbrains.sign-plugin.password") })
  }

  withType<KotlinCompile> {
    kotlinOptions {
      freeCompilerArgs = listOf("-Xjsr305=strict")
      jvmTarget = "17"
    }
  }

  withType<Test> {
    useJUnitPlatform()
  }
}