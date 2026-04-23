import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.ktor)
  alias(libs.plugins.kotlinSerialization)
  alias(libs.plugins.metro)
  application
}

group = "com.wardellbagby.thebeehive"

version = "1.0.0"

application {
  mainClass.set("com.wardellbagby.thebeehive.ApplicationKt")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
  implementation(projects.networking.server)
  implementation(projects.networking.client)
  implementation(libs.logback)
  implementation(libs.ktor.server.core)
  implementation(libs.ktor.server.netty)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.hoplite.core)
  implementation(libs.hoplite.yaml)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.cio)
  implementation(libs.ktor.client.contentNegotiation)
  implementation(libs.ktor.server.contentNegotiation)
  implementation(libs.ktor.server.websockets)
  implementation(libs.ktor.serializationKotlinxJson)
  implementation(libs.ktor.clientLogging)
  implementation(libs.kotlinx.datetime)
  implementation(libs.firebase.admin)
  implementation(libs.jmdns)
  implementation(libs.kotlin.process)
  testImplementation(libs.ktor.server.testHost)
  testImplementation(libs.kotlin.testJunit)
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.compilerOptions { freeCompilerArgs.set(listOf("-Xcontext-parameters")) }
