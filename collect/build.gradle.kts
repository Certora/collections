import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*

plugins {
	kotlin("jvm")
	id("io.github.detekt.gradle.compiler-plugin")
    id("java-library")
}

kotlin {
    explicitApi()
}

detekt {
    config.setFrom("detekt.yml")
}

dependencies {
	implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:${property("immutableCollectionsVersion")}")
    detektPlugins(project(":detekt-treapability"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
	testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${property("serializationVersion")}")
}
