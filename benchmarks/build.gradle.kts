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
            warmups = 5
            iterations = 5
            iterationTime = 500
            iterationTimeUnit = "ms"
            param("size", "1", "10", "100", "10000")
            param("immutablePercentage", "0")
            param("hashCodeType", "random", "collision")
        }

        register("fast") {
            warmups = 5
            iterations = 5
            iterationTime = 500
            iterationTimeUnit = "ms"
            param("size", "1000")
            param("immutablePercentage", "0")
            param("hashCodeType", "random")

            include("immutableMap.Get.get$")
            include("immutableMap.Iterate.iterateKeys$")
            include("immutableMap.Put.put$")
            include("immutableMap.Remove.remove$")

            include("immutableSet.Add.add$")
            include("immutableSet.Contains.contains$")
            include("immutableSet.Iterate.iterate$")
            include("immutableSet.Remove.remove$")
        }

        register("setIteration") {
            warmups = 5
            iterations = 5
            iterationTime = 500
            iterationTimeUnit = "ms"
            param("size", "1000")
            param("hashCodeType", "random")

            include("immutableSet.Iterate.iterate$")
            include("immutableSet.ForEachElement.iterate$")
        }

        register("setOperators") {
            warmups = 5
            iterations = 5
            iterationTime = 500
            iterationTimeUnit = "ms"
            param("size", "10", "10000")
            param("hashCodeType", "random")

            include("immutableSet.SetOperators")
        }

        register("mapMerge") {
            warmups = 5
            iterations = 5
            iterationTime = 500
            iterationTimeUnit = "ms"
            param("size", "10", "10000")
            param("hashCodeType", "random")

            include("immutableMap.Merge")
            include("immutableMap.ParallelMerge")
        }

        register("mapUpdateValues") {
            warmups = 5
            iterations = 5
            iterationTime = 500
            iterationTimeUnit = "ms"
            param("size", "10", "10000")
            param("hashCodeType", "random")

            include("immutableMap.UpdateValues")
            include("immutableMap.ParallelUpdateValues")
        }
    }
}


task("sizesBenchmark", JavaExec::class) {
    main = "benchmarks.SizesKt"
    args = listOf("$buildDir/reports/benchmarks/size")
    classpath = sourceSets["main"].runtimeClasspath
}
