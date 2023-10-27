package benchmarks.size

import benchmarks.*
import com.certora.collect.*
import kotlinx.collections.immutable.*
import kotlinx.serialization.Serializable
import org.openjdk.jol.info.GraphLayout
import java.util.IdentityHashMap

data class SetCase(
    val scenario: String,
    val scenarioSize: Int,
    val hashSet: Sequence<PersistentSet<Any>>,
    val persistentHashSet: Sequence<PersistentSet<Any>>,
    val persistentOrderedSet: Sequence<PersistentSet<Any>>,
    val hashTreapSet: Sequence<PersistentSet<Any>>,
    val sortedTreapSet: Sequence<PersistentSet<Any>>,
) {
    class Context(
        val empty: () -> PersistentSet<Any>,
        val key: (Int) -> Any
    )

    constructor(scenario: String, scenarioSize: Int, test: Context.() -> Sequence<PersistentSet<Any>>) : this(
        scenario = scenario,
        scenarioSize = scenarioSize,
        hashSet = Context({ fakePersistentSetOf() }, { HashableSizeKey(it) }).test(),
        persistentHashSet = Context({ persistentHashSetOf() }, { HashableSizeKey(it) }).test(),
        persistentOrderedSet = Context({ persistentSetOf() }, { HashableSizeKey(it) }).test(),
        hashTreapSet = Context({ treapSetOf() }, { HashableSizeKey(it) }).test(),
        sortedTreapSet = Context({ treapSetOf() }, { ComparableSizeKey(it) }).test(),
    )

    @Serializable
    data class Sizes(
        val scenario: String,
        val scenarioSize: Int,
        val hashSet: Long,
        val persistentHashSet: Long,
        val persistentOrderedSet: Long,
        val hashTreapSet: Long,
        val sortedTreapSet: Long,
    )

    private fun Sequence<Set<*>>.computeSize(): Long {
        val unwrapped = this.map { (it as? FakePersistentSet<*>)?.value ?: it }.toList()

        val keys = IdentityHashMap<Any, Any>()
        unwrapped.forEach {
            it.forEach { keys[it] = DummyValue }
        }

        return GraphLayout.parseInstance(*unwrapped.toTypedArray())
            .subtract(GraphLayout.parseInstance(*keys.keys.toTypedArray()))
            .totalSize()
    }

    val sizes get() = Sizes(
        scenario = scenario,
        scenarioSize = scenarioSize,
        hashSet = hashSet.computeSize(),
        persistentHashSet = persistentHashSet.computeSize(),
        persistentOrderedSet = persistentOrderedSet.computeSize(),
        hashTreapSet = hashTreapSet.computeSize(),
        sortedTreapSet = sortedTreapSet.computeSize(),
    )
}
