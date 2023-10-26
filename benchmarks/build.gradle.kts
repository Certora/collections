import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.*

plugins {
	kotlin("jvm")
    kotlin("plugin.serialization")
    kotlin("plugin.allopen")
    id("java-library")
    id("org.jetbrains.kotlinx.benchmark")
}


allOpen {
    annotation("org.openjdk.jmh.annotations.State")
}

dependencies {
	implementation(kotlin("reflect"))
    implementation("de.brudaswen.kotlinx.serialization:kotlinx-serialization-csv:2.0.0")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:${property("serializationVersion")}")
	implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:${property("immutableCollectionsVersion")}")
    implementation("org.jetbrains.kotlinx:kotlinx-benchmark-runtime:${property("benchmarkVersion")}")
    implementation("org.openjdk.jol:jol-core:0.17")
    implementation(project(":collect"))
}

benchmark {
    targets {
        register("main")
    }

    configurations {
        named("main") {
            param("size", "10", "1000", "10000")
            param("hashCodeType", "random")
            param("implementation", "treap")
        }

        register("compare") {
            param("size", "10", "1000", "10000")
            param("hashCodeType", "random")
            param("implementation", "hash_map", "hamt", "treap")
        }

        register("named") {
            param("size", "10", "1000", "10000")
            param("hashCodeType", "random")
            param("implementation", "hash_map", "hamt", "treap")
            include("${project.findProperty("benchmark")}")
        }

        register("mapMerge") {
            param("size", "10", "10000")
            param("hashCodeType", "random")
            param("implementation", "treap")

            include("immutableMap.Merge")
            include("immutableMap.ParallelMerge")
        }

        register("mapUpdateValues") {
            param("size", "10", "10000")
            param("hashCodeType", "random")
            param("implementation", "treap")

            include("immutableMap.UpdateValues")
            include("immutableMap.ParallelUpdateValues")
        }

        configureEach {
            warmups = 5
            iterations = 10
            iterationTime = 100
            iterationTimeUnit = "ms"
            param("immutablePercentage", "0")
        }
    }
}


task("sizesBenchmark", JavaExec::class) {
    main = "benchmarks.size.RunnerKt"
    jvmArgs = listOf("-Djdk.attach.allowAttachSelf", "-XX:+EnableDynamicAgentLoading")
    args = listOf("$buildDir/reports/benchmarks/size")
    classpath = sourceSets["main"].runtimeClasspath
}
