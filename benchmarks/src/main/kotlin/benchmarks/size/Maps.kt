package benchmarks.size

import benchmarks.*
import kotlinx.collections.immutable.*

val maps = sequence {
    yield(MapCase("Empty", 0) { sequenceOf(empty()) })
    scenarioSizes.forEach {
        yield(MapCase("Fresh", it) { sequenceOf(empty() + (1..it).map { key(it) to DummyValue }) })
    }
}
