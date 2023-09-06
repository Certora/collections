package com.certora.common.collect

/**
 * Marker interface for classes with "stable" hash codes.  Such classes have [Object.hashCode] implementations whose
 * results do not vary across JVM instances.
 *
 * Hash code stability is important in a couple of scenarios:
 *
 * 1) Our PersistentMap/PersistentSet implementations rely on the keys' hash codes to balance the underlying Treap
 *    data structures.  When these are deserialized, we need the resulting objects' hash codes to be the same as they
 *    were when serialized.
 * 2) We need stable hash codes in other cases, as part of caching compilation and verification results.
 *
 * The 'HashCodeStability' Detekt rule enforces hash code stability for classes which implement [StableHashCode].
 *
 * Some notes about hash code stability for various types:
 *
 * o The default [Object.hashCode] implementation does not produce a stable hash code; it effecitvely assigns a random
 *   number to each class instance.  A "naked" class that does not override hashCode will not have a stable hash code.
 *
 * o Kotlin/JVM primitive types (Int, Char, etc.) do have stable hash codes.
 *
 * o Kotlin/JVM Strings have stable hash codes.
 *
 * o Kotlin "data classes" provide a hashCode implementation which is stable as long as the properties in the
 *   data class' primary constructor all have stable hash codes.
 *
 * o Kotlin "object" types do not provide stable hash codes by default; they get the random number from the underlying
 *   [Object.hashCode] implementation.
 *
 * o Classes which implement [Collection] or [Map] are required to have stable hash codes (as part of the semantics of
 *   those interfaces), so long as the elements stored in those collections also have stable hash codes.
 *
 * o Kotlin/JVM "enum classes" do not have stable hash codes.  They use the default [Object.hashCode] implementation,
 *   and do not allow this to be overridden.  A stable hash code can be obtained from the *name* of the enum instance.
 *   (We could also use the ordinal, but this has an unnecessary dependence on ordering.)
 *
 * o Our analysis considers some known JCL types to have stable hash codes (such as BigIngeter).
 */
interface StableHashCode
