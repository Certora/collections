package com.certora.collect

internal fun <T> Sequence<T>.foldFirstOrNull(f: (T, T) -> T) : T? {
    val it = this.iterator()
    if(!it.hasNext()) {
        return null
    }
    var start = it.next()
    while(it.hasNext()) {
        start = f(start, it.next())
    }
    return start
}
