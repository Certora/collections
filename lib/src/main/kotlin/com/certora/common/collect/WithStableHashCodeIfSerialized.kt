package com.certora.common.collect

/**
 * Marks a generic type parameter as needing stable hash codes *if* the type might be involved in serialization.
 * NOTE: It exists specifically for generic collection types (see AbstractTreapSet example below)!
 *
 * Such a parameter will accept an argument that either a) is definitely not serializable, or b) has a stable hash code.
 *
 * A type is definitely not serializable if it doesn't implement Serializable *and* it is final or sealed - and all
 * subclasses are definitely not serializable.
 *
 * This allows collection types to rely on stable hash codes for serialization, but otherwise not require them.  E.g.:
 *
 *      class AbstractTreapSet<@WithStableHashCodeIfSerialized T>(): Serializable
 *
 * `AbstractTreapSet` can rely on hashCode stability in its serialization code, because if T is serializable, it also has a
 * stable hash code.  But we're free to instantiate `AbstractTreapSet<UnstableType>`, as long as `UnstableType` definitely not
 * serializable.
 */
@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE)
annotation class WithStableHashCodeIfSerialized
