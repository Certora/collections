package benchmarks

import kotlinx.collections.immutable.*

class FakePersistentMap<K, V>(val value: Map<K, V>) : PersistentMap<K, V>, Map<K, V> by value {
    class Builder<K, V>(val value: MutableMap<K, V>) : PersistentMap.Builder<K, V>, MutableMap<K, V> by value {
        override fun equals(other: Any?) = value == other
        override fun hashCode() = value.hashCode()
        override fun build() = FakePersistentMap(value)
    }

    override fun equals(other: Any?) = value == other
    override fun hashCode() = value.hashCode()

    override fun clear() = FakePersistentMap(emptyMap<K, V>())
    override fun builder() = Builder(value.toMutableMap())
    override fun put(key: K, value: V) = FakePersistentMap(this.value + (key to value))
    override fun putAll(m: Map<out K, V>) = FakePersistentMap(value + m)
    override fun remove(key: K) = FakePersistentMap(value - key)
    override fun remove(key: K, value: V) = mutate { it.remove(key, value) }

    override val entries get() = value.entries.toImmutableSet()
    override val keys get() = value.keys.toImmutableSet()
    override val values get() = value.values.toImmutableList()
}

fun <K, V> fakePersistentMapOf(): PersistentMap<K, V> = FakePersistentMap(emptyMap<K, V>())
