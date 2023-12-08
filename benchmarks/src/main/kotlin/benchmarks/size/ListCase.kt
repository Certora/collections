package benchmarks.size

import benchmarks.*
import com.certora.collect.*
import kotlinx.collections.immutable.*
import kotlinx.serialization.Serializable
import org.openjdk.jol.info.GraphLayout
import java.util.IdentityHashMap

data class ListCase(
    val scenario: String,
    val scenarioSize: Int,
    val javaList: Sequence<PersistentList<Any>>,
    val persistentList: Sequence<PersistentList<Any>>,
    val treapList: Sequence<PersistentList<Any>>,
) {
    class Context(
        val empty: () -> PersistentList<Any>,
        val key: (Int) -> Any
    )

    constructor(scenario: String, scenarioSize: Int, test: Context.() -> Sequence<PersistentList<Any>>) : this(
        scenario = scenario,
        scenarioSize = scenarioSize,
        javaList = Context({ fakePersistentListOf() }, { HashableSizeKey(it) }).test(),
        persistentList = Context({ persistentListOf() }, { HashableSizeKey(it) }).test(),
        treapList = Context({ treapListOf() }, { HashableSizeKey(it) }).test(),
    )

    @Serializable
    data class Sizes(
        val scenario: String,
        val scenarioSize: Int,
        val javaList: Long,
        val persistentList: Long,
        val treapList: Long,
    )

    private fun Sequence<List<*>>.computeSize(): Long {
        val unwrapped = this.map { (it as? FakePersistentList<*>)?.value ?: it }.toList()

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
        javaList = javaList.computeSize(),
        persistentList = persistentList.computeSize(),
        treapList = treapList.computeSize(),
    )
}
