package benchmarks.size

import benchmarks.*
import com.certora.collect.*
import kotlinx.collections.immutable.*
import kotlinx.serialization.Serializable
import org.openjdk.jol.info.GraphLayout
import java.util.IdentityHashMap

data class MapCase(
    val scenario: String,
    val scenarioSize: Int,
    val hashMap: Sequence<PersistentMap<Any, Any>>,
    val persistentHashMap: Sequence<PersistentMap<Any, Any>>,
    val hashTreapMap: Sequence<PersistentMap<Any, Any>>,
    val sortedTreapMap: Sequence<PersistentMap<Any, Any>>,
) {
    class Context(
        val empty: () -> PersistentMap<Any, Any>,
        val key: (Int) -> Any
    )

    constructor(scenario: String, scenarioSize: Int, test: Context.() -> Sequence<PersistentMap<Any, Any>>) : this(
        scenario = scenario,
        scenarioSize = scenarioSize,
        hashMap = Context({ fakePersistentMapOf() }, { HashableSizeKey(it) }).test(),
        persistentHashMap = Context({ persistentHashMapOf() }, { HashableSizeKey(it) }).test(),
        hashTreapMap = Context({ treapMapOf() }, { HashableSizeKey(it) }).test(),
        sortedTreapMap = Context({ treapMapOf() }, { ComparableSizeKey(it) }).test(),
    )

    @Serializable
    data class Sizes(
        val scenario: String,
        val scenarioSize: Int,
        val hashMap: Long,
        val persistentHashMap: Long,
        val hashTreapMap: Long,
        val sortedTreapMap: Long,
    )

    private fun Sequence<Map<*, *>>.computeSize(): Long {
        val unwrapped = this.map { (it as? FakePersistentMap<*, *>)?.value ?: it }.toList()

        val keysAndValues = IdentityHashMap<Any, Any>()
        unwrapped.forEach {
            it.keys.forEach { keysAndValues[it] = DummyValue }
            it.values.forEach { keysAndValues[it] = DummyValue }
        }

        return GraphLayout.parseInstance(*unwrapped.toTypedArray())
            .subtract(GraphLayout.parseInstance(*keysAndValues.keys.toTypedArray()))
            .totalSize()
    }

    val sizes get() = Sizes(
        scenario = scenario,
        scenarioSize = scenarioSize,
        hashMap = hashMap.computeSize(),
        persistentHashMap = persistentHashMap.computeSize(),
        hashTreapMap = hashTreapMap.computeSize(),
        sortedTreapMap = sortedTreapMap.computeSize(),
    )
}

