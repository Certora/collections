import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("io.github.detekt.gradle.compiler-plugin")
    id("java-library")
}

subprojects {
	apply(plugin = "java-library")

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