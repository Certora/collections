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

include("Lib", "DetektRules")
project(":Lib").projectDir = file("lib")
project(":DetektRules").projectDir = file("detekt")
