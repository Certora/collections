@file:Suppress("NOTHING_TO_INLINE")
package com.certora.common.collect

import kotlin.reflect.KClass

/**
 * A mini-DSL for easily generating stable composite object hash codes.  For example:
 *
 *      enum class E { A, B, C }
 *
 *      class Bar(val i: Int, val e: E) {
 *          override fun hashCode() = hash { it + i + e }
 *      }
 *
 * (Note the 'it' reference in the 'hash' block; that's unfortunately necessary to get the HashCode addition chain
 * started.)
 *
 * This automates the usual "multiply and add" composition scheme.  It also provides stable hash codes for some
 * problematic types, such as enums.
 */
inline fun hash(initial: Int = 0, action: (HashCode) -> HashCode) = action(HashCode(initial)).code

@JvmInline value class HashCode(val code: Int) {
    @PublishedApi
    internal inline fun add(obj: Any?) = HashCode(31 * this.code + obj.hashCode())

    inline infix operator fun <@Treapable T> plus(obj: T) = add(obj)
    inline infix operator fun plus(clazz: Class<*>?) = add(clazz?.name)
    inline infix operator fun plus(clazz: KClass<*>?) = add(clazz?.java?.name)
    inline infix operator fun plus(e: Enum<*>?) = add(e?.name)
}


/**
 * Helper to generate a stable hash code for a Kotlin "object."  For example:
 *
 *      object Foo : HashStableHash {
 *          override fun hashCode() = hashObject(this)
 *      }
 */
inline fun hashObject(obj: Any) = hash { it + obj::class }

