package com.certora.collect

/**
 * Faster iteration than [kotlin.collections.forEach], for our Map implementations.
 * We'd like to just call this `forEach` so it would automatically take over from the Kotlin version.  But Kotlin
 * uses strange overload resolution rules for kotlin.collections.forEach (see the `HidesMembers` annotation here:
 * https://github.com/JetBrains/kotlin/blob/30788566012c571aa1d3590912468d1ebe59983d/libraries/stdlib/common/src/generated/_Maps.kt#L214)
 *
 * So instead, we have to call this by a different name.  We have a compile-time warning about this, generated by
 * [com.certora.detekt.ImportStdCollections].
 */
@Suppress("UNCHECKED_CAST")
public inline fun <K, V> Map<out K, V>.forEachEntry(action: (Map.Entry<K, V>) -> Unit) {
    when (this) {
        is LinkedArrayHashMap<*, *> ->
            (this as LinkedArrayHashMap<K, V>).forEachEntry {
                k, v -> action(MapEntry(k, v))
            }
        is ArrayHashMap<*, *> ->
            (this as ArrayHashMap<K, V>).forEachEntry {
                k, v -> action(MapEntry(k, v))
            }
        is TreapMap.Builder<*, *> ->
            (this as TreapMap.Builder<K, V>).build().forEach(action)
        else ->
            forEach(action)
    }
}
