pluginManagement {
    val kotlinVersion: String by settings
    val detektVersion: String by settings
    val axionVersion: String by settings
    val benchmarkVersion: String by settings
    val dokkaVersion: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
        id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
        id("io.github.detekt.gradle.compiler-plugin") version detektVersion
        id("pl.allegro.tech.build.axion-release") version axionVersion
        id("org.jetbrains.kotlinx.benchmark") version benchmarkVersion
        id("org.jetbrains.dokka") version dokkaVersion
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "Collections"

include("collect", "detekt-treapability", "benchmarks")
