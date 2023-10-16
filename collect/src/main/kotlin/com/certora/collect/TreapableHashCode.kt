@file:Suppress("NOTHING_TO_INLINE")
package com.certora.collect

import kotlin.reflect.KClass

/**
    A mini-DSL for easily generating "treapable" object hash codes.  For example:

        ```
        enum class E { A, B, C }

        class Bar(val i: Int, val e: E) {
            override fun hashCode() = treapHash { it + i + e }
        }
        ```

    (Note the 'it' reference in the 'hash' block; that's unfortunately necessary to get the HashCode addition chain
    started.)

    This automates the usual "multiply and add" composition scheme.  It also provides stable hash codes for some
    problematic types, such as enums.
 */
public inline fun treapHash(initial: Int = 0, action: (TreapableHashCode) -> TreapableHashCode): Int = action(TreapableHashCode(initial)).code

/** 
    See [hash].
 */
public @JvmInline value class TreapableHashCode(@PublishedApi internal val code: Int) {
    @PublishedApi
    internal inline fun add(obj: Any?): TreapableHashCode = TreapableHashCode(31 * this.code + obj.hashCode())

    public inline infix operator fun <@Treapable T> plus(obj: T): TreapableHashCode = add(obj)
    public inline infix operator fun plus(clazz: Class<*>?): TreapableHashCode = add(clazz?.name)
    public inline infix operator fun plus(clazz: KClass<*>?): TreapableHashCode = add(clazz?.java?.name)
    public inline infix operator fun plus(e: Enum<*>?): TreapableHashCode = add(e?.name)
}


/**
    Helper to generate a stable hash code for a Kotlin object.  For example:

        ```
        object Foo : HashStableHash {
            override fun hashCode() = treapHashObject(this)
        }
        ```
 */
public inline fun treapHashObject(obj: Any): Int = treapHash { it + obj::class }

