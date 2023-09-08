pluginManagement {
    val kotlinVersion: String by settings
    val detektVersion: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
        kotlin("plugin.serialization") version kotlinVersion
        id("io.github.detekt.gradle.compiler-plugin") version detektVersion
    }
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "Collections"

include("common-collections", "common-collections-detekt")
project(":common-collections").projectDir = file("common-collections")
project(":common-collections-detekt").projectDir = file("common-collections-detekt")
