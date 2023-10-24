import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.base.DokkaBaseConfiguration
import java.net.URL

plugins {
	kotlin("jvm")
	kotlin("plugin.serialization")
	id("io.github.detekt.gradle.compiler-plugin")
    id("java-library")
    id("maven-publish")
    id("org.jetbrains.dokka")
}

buildscript {
    dependencies {
        classpath("org.jetbrains.dokka:dokka-base:1.9.10")
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
    detektPlugins(project(":detekt-treapability"))
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
	testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
}

tasks.withType<DokkaTask>().configureEach {
    moduleName.set("Certora Collections")
    //failOnWarning.set(true)
    suppressObviousFunctions.set(true)
    suppressInheritedMembers.set(false)

    pluginConfiguration<DokkaBase, DokkaBaseConfiguration> {
        customAssets = listOf(file("../assets/logo-icon.svg"))
        footerMessage = "(c) Certora"
    }

    dokkaSourceSets.configureEach {
        reportUndocumented.set(true)

        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            remoteUrl.set(URL("https://github.com/Certora/collections/tree/main/collect/src"))
            remoteLineSuffix.set("#L")
        }
    }
}

tasks.register<Jar>("dokkaJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.flatMap { it.outputDirectory })
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifact(tasks["dokkaJar"])
        }
    }
}
