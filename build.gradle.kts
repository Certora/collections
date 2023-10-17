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
}

subprojects {
	apply(plugin = "java-library")
	apply(plugin = "maven-publish")

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

	publishing {
		publications {
			repositories {
				maven {
					name = "GitHubPackages"
					url = URI("https://maven.pkg.github.com/Certora/collections")
					credentials {
						username = System.getenv("GITHUB_ACTOR")
						password = System.getenv("GITHUB_TOKEN")
					}
				}
			}

			withType<MavenPublication> {
				version = if (project.hasProperty("release")) {
					"${project.version}"
				} else {
					"${project.version}-SNAPSHOT"
				}

				pom {
					licenses {
						license {
							name.set("MIT License")
							url.set("https://github.com/Certora/collections/blob/4bc9da2c8197aea0ed3ad8b32b5a3dbcd69e725e/LICENSE")
						}
					}
					scm {
						connection.set("scm:git:https://github.com/Certora/collections.git")
						developerConnection.set("scm:git:ssh://github.com/Certora/collections.git")
						url.set("https://github.com/Certora/collections/")
					}
				}
			}
		}
	}
}
