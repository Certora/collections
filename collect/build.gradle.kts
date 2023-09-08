import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("io.github.detekt.gradle.compiler-plugin")
    id("java-library")
    id("maven-publish")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(property("javaVersion").toString()))
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

publishing {
    publications {
        create<MavenPublication>("certora-common-collections") {
            from(components["java"])
        }
    }
}

dependencies {
	implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
    detektPlugins(project(":detekt-treapability"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
	testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}
