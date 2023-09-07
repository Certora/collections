package com.certora.common.utils

// Use "foo.uncheckedAs<Bar<T>>() in place of "foo as Bar<T>" to avoid @Suppress stuff everywhere
@Suppress("UNCHECKED_CAST", "NOTHING_TO_INLINE")
@PublishedApi
internal inline fun <T> Any?.uncheckedAs(): T = this as T

// Do-nothing function to easily suppress unused parameter/variable warnings
@Suppress("NOTHING_TO_INLINE", "EmptyFunctionBlock")
@PublishedApi
internal inline fun <T> unused(@Suppress("UNUSED_PARAMETER") param: T) { }

@PublishedApi
internal fun <T> Iterator<T>.nextOrNull() = if (hasNext()) { next() } else { null }
