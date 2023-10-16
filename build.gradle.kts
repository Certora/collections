import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("io.github.detekt.gradle.compiler-plugin")
    id("java-library")
	id("com.palantir.git-version") version "3.0.0"
}

subprojects {
	apply(plugin = "java-library")
	apply(plugin = "com.palantir.git-version")

	val versionDetails: groovy.lang.Closure<com.palantir.gradle.gitversion.VersionDetails> by extra
	val details = versionDetails()
	
	if (project.hasProperty("release")) {
		version = details.lastTag
	} else {
		version = "${details.lastTag}-SNAPSHOT"
	}

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