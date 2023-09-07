import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val kotlinVersion: String by project
val javaVersion: String by project
val detektVersion: String by project

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("io.github.detekt.gradle.compiler-plugin")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
    }
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        allWarningsAsErrors = true
        freeCompilerArgs += "-Xcontext-receivers"
    }
}

kotlin {
    explicitApi()
}

detekt {
    config.setFrom("detekt.yml")
}

dependencies {
	implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
    detektPlugins(project(":DetektRules"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
	testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}
