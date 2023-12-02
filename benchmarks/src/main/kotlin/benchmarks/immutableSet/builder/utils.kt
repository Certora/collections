/*
 * Modified from the kotlinx.collections.immutable sources, which contained the following notice:
 * Copyright 2016-2019 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package benchmarks.immutableSet.builder

import benchmarks.*
import benchmarks.immutableSet.emptyPersistentSet
import kotlinx.collections.immutable.PersistentSet

fun <E> persistentSetBuilderAdd(
        implementation: String,
        elements: List<E>,
        immutablePercentage: Double
): PersistentSet.Builder<E> {
    val immutableSize = immutableSize(elements.size, immutablePercentage)

    var set = emptyPersistentSet<E>(implementation)
    for (index in 0 until immutableSize) {
        set = set.add(elements[index])
    }

    val builder = set.builder()
    for (index in immutableSize until elements.size) {
        builder.add(elements[index])
    }

    return builder
}
