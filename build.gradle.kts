import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("io.github.detekt.gradle.compiler-plugin")
    id("java-library")
	id("maven-publish")
    id("pl.allegro.tech.build.axion-release")
}

subprojects {
	apply(plugin = "java-library")
	apply(plugin = "maven-publish")
    apply(plugin = "pl.allegro.tech.build.axion-release")

    group = "com.github.certora.collections"
    version = scmVersion.version

	repositories {
		mavenCentral()
	}

	java {
		toolchain {
			languageVersion.set(JavaLanguageVersion.of(property("javaVersion").toString()))
		}
	}

	tasks.register("buildAndPublishToMavenLocal") {
		dependsOn("build")
		dependsOn("publishToMavenLocal")
	}

	tasks.withType<KotlinCompile> {
		kotlinOptions {
			allWarningsAsErrors = true
			freeCompilerArgs += "-Xcontext-receivers"
		}
	}
}
