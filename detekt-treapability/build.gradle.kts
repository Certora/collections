import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
    id("java-library")
    id("maven-publish")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging.showStandardStreams = true
}

publishing {
    publications {
        create<MavenPublication>("certora-common-collections-detekt") {
            from(components["java"])
        }
    }
}

dependencies {
    val detektVersion: String by project
    val junitVersion: String by project
    compileOnly("io.gitlab.arturbosch.detekt:detekt-api:$detektVersion")
    compileOnly("io.gitlab.arturbosch.detekt:detekt-core:$detektVersion")
    testImplementation("io.gitlab.arturbosch.detekt:detekt-test:$detektVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}
