import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import java.net.URI

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("io.github.detekt.gradle.compiler-plugin")
    id("java-library")
    id("maven-publish")
}

kotlin {
    explicitApi()
}

detekt {
    config.setFrom("detekt.yml")
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

        create<MavenPublication>("certora-collections") {
            from(components["java"])

            pom {
                name.set("Certrora Collections")
                description.set("Efficient collection types for Kotlin")
                url.set("http://www.github.com/Certora/collections")
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

dependencies {
	implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
    detektPlugins(project(":detekt-treapability"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
	testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}
