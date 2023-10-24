pluginManagement {
    val kotlinVersion: String by settings
    val detektVersion: String by settings
    val axionVersion: String by settings
    val dokkaVersion: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("io.github.detekt.gradle.compiler-plugin") version detektVersion
        id("pl.allegro.tech.build.axion-release") version axionVersion
        id("org.jetbrains.dokka") version dokkaVersion
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "Collections"

include("collect", "detekt-treapability")
