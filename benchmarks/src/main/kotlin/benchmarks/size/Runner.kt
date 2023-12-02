@file:OptIn(ExperimentalSerializationApi::class)
package benchmarks.size

import java.nio.file.*
import kotlinx.serialization.*
import kotlinx.serialization.csv.*

/** Computes the sizes of various sets and maps, for comparison. Invoked by the `sizesBenchmark` Gradle task. */
fun main(args: Array<String>) {
    val outputDir = Path.of(args[0])
    Files.createDirectories(outputDir)

    val csv = Csv { hasHeaderRecord = true }


    println("Computing set sizes...")
    val sizes = sets.map { it.sizes }.toList().groupBy { it.scenario }
    sizes.forEach {
        val setsFile = outputDir.resolve("sets-${it.key}.csv")
        Files.writeString(
            setsFile,
            csv.encodeToString(
                it.value.toList()
            )
        )
        println("Wrote $setsFile")
    }

    println("Computing map sizes...")
    val mapSizes = maps.map { it.sizes }.toList().groupBy { it.scenario }
    mapSizes.forEach {
        val mapsFile = outputDir.resolve("maps-${it.key}.csv")
        Files.writeString(
            mapsFile,
            csv.encodeToString(
                it.value.toList()
            )
        )
        println("Wrote $mapsFile")
    }
}

