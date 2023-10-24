import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*

plugins {
	kotlin("jvm")
    kotlin("plugin.allopen")
    id("java-library")
    id("org.jetbrains.kotlinx.benchmark")
}


allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

dependencies {
	implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.3.5")
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:${property("benchmarkVersion")}")
    implementation(project(":collect"))
}

benchmark {
    targets {
        register("main")
    }

    configurations {
        named("main") {
            
        }
        register("smoke") {

        }
    }
}
