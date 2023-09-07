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
public inline fun hash(initial: Int = 0, action: (HashCode) -> HashCode): Int = action(HashCode(initial)).code

public @JvmInline value class HashCode(@PublishedApi internal val code: Int) {
    @PublishedApi
    internal inline fun add(obj: Any?): HashCode = HashCode(31 * this.code + obj.hashCode())

    public inline infix operator fun <@Treapable T> plus(obj: T): HashCode = add(obj)
    public inline infix operator fun plus(clazz: Class<*>?): HashCode = add(clazz?.name)
    public inline infix operator fun plus(clazz: KClass<*>?): HashCode = add(clazz?.java?.name)
    public inline infix operator fun plus(e: Enum<*>?): HashCode = add(e?.name)
}


/**
 * Helper to generate a stable hash code for a Kotlin "object."  For example:
 *
 *      object Foo : HashStableHash {
 *          override fun hashCode() = hashObject(this)
 *      }
 */
public inline fun hashObject(obj: Any): Int = hash { it + obj::class }

