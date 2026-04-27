import com.ncorti.ktfmt.gradle.KtfmtExtension

plugins {
  // this is necessary to avoid the plugins to be loaded multiple times
  // in each subproject's classloader
  alias(libs.plugins.androidApplication) apply false
  alias(libs.plugins.androidLibrary) apply false
  alias(libs.plugins.composeMultiplatform) apply false
  alias(libs.plugins.composeCompiler) apply false
  alias(libs.plugins.kotlinJvm) apply false
  alias(libs.plugins.kotlinMultiplatform) apply false
  alias(libs.plugins.ktor) apply false
  alias(libs.plugins.ktfmt) apply false
  alias(libs.plugins.metro) apply false
  alias(libs.plugins.googleServices) apply false
  alias(libs.plugins.ksp) apply false
  alias(libs.plugins.androidLint) apply false
  alias(libs.plugins.gradleNode) apply false
}

subprojects {
  apply(plugin = "com.ncorti.ktfmt.gradle")
  configure<KtfmtExtension> { googleStyle() }
}

tasks.register<Exec>("configureGitHooks") {
  commandLine("git", "config", "core.hooksPath", ".githooks")
}
