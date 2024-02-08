package benchmarks.size

import benchmarks.*
import kotlinx.collections.immutable.*

val lists = sequence {
    yield(ListCase("Empty", 0) { sequenceOf(empty()) })
    scenarioSizes.forEach {
        yield(ListCase("Fresh", it) { sequenceOf((1..it).toTestList()) })
    }
    scenarioSizes.forEach { yield(ListCase("Append", it) { sequence {
        val fresh = (1..it).toTestList()
        yield(fresh)
        repeat(16) {
            yield(fresh)
        }
    }})}
    scenarioSizes.forEach { yield(ListCase("AddLast", it) { sequence {
        val fresh = (1..it).toTestList()
        yield(fresh)
        repeat(16) {
            yield(fresh.add("hello"))
        }
    }})}
    scenarioSizes.forEach { it -> yield(ListCase("AddFirst", it) { sequence {
        val fresh = (1..it).toTestList()
        yield(fresh)
        repeat(16) {
            yield(fresh.add(0, "hello"))
        }
    }})}
    scenarioSizes.forEach { it -> yield(ListCase("AddMiddle", it) { sequence {
        val fresh = (1..it).toTestList()
        yield(fresh)
        repeat(16) {
           yield(fresh.add(fresh.size / 2, "hello"))
        }
    }})}
    scenarioSizes.forEach { it -> yield(ListCase("SetMiddle", it) { sequence {
        val fresh = (1..it).toTestList()
        yield(fresh)
        repeat(16) {
            yield(fresh.set(fresh.size / 2, "hello"))
        }
    }})}
    scenarioSizes.forEach { it -> yield(ListCase("SetFirst", it) { sequence {
        val fresh = (1..it).toTestList()
        yield(fresh)
        repeat(16) {
            yield(fresh.set(0, "hello"))
        }
    }})}
    scenarioSizes.forEach { it -> yield(ListCase("SetLast", it) { sequence {
        val fresh = (1..it).toTestList()
        yield(fresh)
        repeat(16) {
            yield(fresh.set(fresh.lastIndex, "hello"))
        }
    }})}
}

context(ListCase.Context)
private fun IntRange.toTestList(): PersistentList<Any> = empty() + this.map { key(it) }
