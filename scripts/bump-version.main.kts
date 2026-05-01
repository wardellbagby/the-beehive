#!/usr/bin/env kotlin

import java.io.File

val bumpType = args.firstOrNull()?.lowercase()
if (bumpType == null || bumpType !in listOf("patch", "minor", "major")) {
    System.err.println("Usage: kotlin scripts/bump-version.main.kts <patch|minor|major>")
    kotlin.system.exitProcess(1)
}

val root = File(System.getProperty("user.dir"))

val versionsToml = root.resolve("gradle/libs.versions.toml")
val versionsContent = versionsToml.readText()
val currentVersion =
    Regex("""^beehive = "(.+?)"""", RegexOption.MULTILINE).find(versionsContent)?.groupValues?.get(1)
        ?: error("Could not find beehive version in gradle/libs.versions.toml")

val parts = currentVersion.split(".")
require(parts.size == 3) { "Expected semver X.Y.Z, got: $currentVersion" }
var major = parts[0].toInt()
var minor = parts[1].toInt()
var patch = parts[2].toInt()

when (bumpType) {
    "major" -> {
        major++
        minor = 0
        patch = 0
    }
    "minor" -> {
        minor++
        patch = 0
    }
    "patch" -> patch++
}

val newVersion = "$major.$minor.$patch"
println("Bumping $currentVersion → $newVersion")

val androidBuild = root.resolve("androidApp/build.gradle.kts")
val androidContent = androidBuild.readText()
val androidVersionCode =
    Regex("""versionCode = (\d+)""").find(androidContent)?.groupValues?.get(1)?.toInt()
        ?: error("Could not find versionCode in androidApp/build.gradle.kts")

val xcconfig = root.resolve("iosApp/Configuration/Config.xcconfig")
val xcconfigContent = xcconfig.readText()
val iosVersionCode =
    Regex("""CURRENT_PROJECT_VERSION=(\d+)""").find(xcconfigContent)?.groupValues?.get(1)?.toInt()
        ?: error("Could not find CURRENT_PROJECT_VERSION in iosApp/Configuration/Config.xcconfig")

val newAndroidVersionCode = androidVersionCode + 1
val newIosVersionCode = iosVersionCode + 1
println("Android versionCode: $androidVersionCode → $newAndroidVersionCode")
println("iOS CURRENT_PROJECT_VERSION: $iosVersionCode → $newIosVersionCode")

versionsToml.writeText(
    versionsContent.replace(
        Regex("""^beehive = ".+?"""", RegexOption.MULTILINE),
        """beehive = "$newVersion"""",
    )
)

androidBuild.writeText(
    androidContent
        .replace(Regex("""versionCode = \d+"""), "versionCode = $newAndroidVersionCode")
        .replace(Regex("""versionName = ".+?""""), """versionName = "$newVersion"""")
)

xcconfig.writeText(
    xcconfigContent
        .replace(Regex("""CURRENT_PROJECT_VERSION=\d+"""), "CURRENT_PROJECT_VERSION=$newIosVersionCode")
        .replace(Regex("""MARKETING_VERSION=\S+"""), "MARKETING_VERSION=$newVersion")
)

val serverBuild = root.resolve("server/build.gradle.kts")
serverBuild.writeText(
    serverBuild.readText().replace(
        Regex("""^version = ".+?"""", RegexOption.MULTILINE),
        """version = "$newVersion"""",
    )
)

val packageJson = root.resolve("homebridgePlugin/package.json")
packageJson.writeText(
    packageJson.readText().replace(Regex(""""version": ".+?""""), """"version": "$newVersion"""")
)

fun runCommand(vararg cmd: String) {
    val exit = ProcessBuilder(*cmd).directory(root).inheritIO().start().waitFor()
    if (exit != 0) error("Command failed (exit $exit): ${cmd.joinToString(" ")}")
}

runCommand(
    "git",
    "add",
    "gradle/libs.versions.toml",
    "androidApp/build.gradle.kts",
    "iosApp/Configuration/Config.xcconfig",
    "server/build.gradle.kts",
    "homebridgePlugin/package.json",
)
runCommand("git", "commit", "-m", "chore: bump version to v$newVersion")
runCommand("git", "tag", "v$newVersion")
runCommand("git", "push")
runCommand("git", "push", "--tags")

println("Done! Tagged v$newVersion and pushed.")
