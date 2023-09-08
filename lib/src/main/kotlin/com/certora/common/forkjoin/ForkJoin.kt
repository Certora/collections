package com.certora.common.forkjoin

import java.util.concurrent.*

/**
    Efficient abstraction of fork/join parallelism.  The idea is to make it easy to implement structured fork/join
    parallel algorithms, e.g. parallel tree traversals, and to be able to reuse the same code for both parallel and
    *efficient* sequential execution. Forking (or not) is controlled by a [Forker] which is passed to the function being
    executed.  Anywhere the function should fork in parallel mode, it calls [fork] with the code that might be executed
    in parallel.  When run sequentially, these [fork] calls simply invoke the code inline, without any real overhead.

    Forker contexts are established using the [forking] and [noForking] functions.

    This is very convenient in concert with Kotlin's context receiver syntax, which allows the context to be passed
    implicitly, so that the implementation can focus on the algorithm, and not on passing the context around.

    ```
    class Foo {
        fun doStuf() = notForking { doStuffImpl() }

        fun doStuffParallel() = forking { doStuffImpl() }

        context(Forker)
        fun doStuffImpl() = fork(
            { doOneThing() }
            { doAnotherThing() }
        }
    }
    ```

    See [com.certora.common.collect.AbstractTreapMap.parallelUpdateValues] for a more realistic example, which includes
    using [ThresholdForker] to improve the efficiency of the parallel case.
 */
public sealed class Forker private constructor(
    @PublishedApi internal val forking: Boolean
) {
    @PublishedApi internal object Forking : Forker(true)
    @PublishedApi internal object NotForking : Forker(false)

    public inline fun <R1, R2> fork(
        crossinline f1: () -> R1,
        crossinline f2: () -> R2
    ): Pair<R1, R2> = when {
        // If we're not forking, just run the functions sequentially, inline, right here.
        !forking -> Pair(f1(), f2())
        else -> {
            val t1 = forkTask { f1() }
            val v2 = f2()
            val v1 = t1.join()
            Pair(v1, v2)
        }
    }

    public inline fun <R1, R2, R3> fork(
        crossinline f1: () -> R1,
        crossinline f2: () -> R2,
        crossinline f3: () -> R3
    ): Triple<R1, R2, R3> = when {
        !forking -> Triple(f1(), f2(), f3())
        else -> {
            // From the JDK docs (https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ForkJoinTask.html):
            // "...returns (joins) should be performed innermost-first."
            val t1 = forkTask { f1() }
            val t2 = forkTask { f2() }
            val v3 = f3()
            val v2 = t2.join()
            val v1 = t1.join()
            Triple(v1, v2, v3)
        }
    }
}

/**
    Runs the function [f] in a [Forking] context that will fork parallel operations.
 */
public fun <R> forking(f: context(Forker)() -> R): R = f(Forker.Forking)

/**
    Runs the function [f] in a [Forking] context that will not fork.
 */
public inline fun <R> notForking(f: context(Forker)() -> R): R = f(Forker.NotForking)


/**
    Like [Forker], but only forks up to the point where [threshold] returns true.  For example, if traversiving a tree
    structure, [threshold] might return true when a subtree is small enough that it is more efficient to process it
    sequentially.

    ThresholdForker contexts are established using the [maybeForking] or [noForking] functions.

    See See [datasturctures.treap.AbstractTreapMap.parallelUpdateValues] for a usage example.
 */
public open class ThresholdForker<in T> @PublishedApi internal constructor(
    @PublishedApi internal val threshold: (T) -> Boolean
) {
    @PublishedApi internal object NotForking : ThresholdForker<Any?>({ true })

    public inline fun <R1, R2> fork(
        currentObj: T,
        crossinline f1: context(ThresholdForker<T>)() -> R1,
        crossinline f2: context(ThresholdForker<T>)() -> R2
    ): Pair<R1, R2> = when {
        // If we've reached the threshold, run the rest of the forks in a NotForking context.
        threshold(currentObj) -> Pair(f1(NotForking), f2(NotForking))
        else -> Forker.Forking.fork({ f1(this) }, { f2(this) })
    }

    public inline fun <R1, R2, R3> fork(
        currentObj: T,
        crossinline f1: context(ThresholdForker<T>)() -> R1,
        crossinline f2: context(ThresholdForker<T>)() -> R2,
        crossinline f3: context(ThresholdForker<T>)() -> R3
    ): Triple<R1, R2, R3> = when {
        threshold(currentObj) -> Triple(f1(NotForking), f2(NotForking), f3(NotForking))
        else -> Forker.Forking.fork({ f1(this) }, { f2(this) }, { f3(this) })
    }
}

/**
    Runs the function [f] in a [ThresholdForker] context that will fork parallel operations until [threshold] returns
    true.
 */
public fun <T, R> maybeForking(
    currentObject: T,
    threshold: (T) -> Boolean,
    f: context(ThresholdForker<T>)() -> R
): R = when {
    threshold(currentObject) -> { f(ThresholdForker.NotForking) }
    else -> forkTask { f(ThresholdForker(threshold)) }.join()
}

/**
    Runs the function [f] in a [ThresholdForker] context that will not fork.
 */
public inline fun <T, R> notForking(
    @Suppress("unused_parameter") currentObject: T, // used to disambiguate between the two notForking functions
    f: context(ThresholdForker<T>)() -> R
): R = f(ThresholdForker.NotForking)


/**
    Helper to start a new [ForkJoinTask] that executes the given function [f].
 */
@PublishedApi internal fun <T> forkTask(f: () -> T): ForkJoinTask<T> = object : RecursiveTask<T>() {
    override fun compute(): T = f()
}.fork()
