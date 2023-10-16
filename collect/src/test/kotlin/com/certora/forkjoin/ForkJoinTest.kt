package com.certora.forkjoin

import kotlin.test.*

/** Tests for com.certora.forkjoin.*. */
class ForkJoinTest {
    context(Forker)
    fun noThreshold() = fork(
        { 1 + 1 },
        { 2 + 2 }
    )

    @Test
    fun forkNoThreshold() {
        forking {
            assertEquals(2 to 4, noThreshold())
        }
    }

    @Test
    fun noForkNoThreshold() {
        notForking {
            assertEquals(2 to 4, noThreshold())
        }
    }

    context(ThresholdForker<Int>)
    fun withThreshold(n: Int) = fork(
        n,
        { 1 + 1 + n },
        { 2 + 2 + n }
    )

    @Test
    fun forkWithThreshold() {
        maybeForking(5, { it > 0 }) {
            assertEquals(2 to 4, withThreshold(0))
        }
    }

    @Test
    fun noForkWithThreshold() {
        notForking(5) {
            assertEquals(2 to 4, withThreshold(0))
        }
    }


    private class TestException : Exception()
    private class TestError : Error()

    @Test
    fun exception() {
        forking {
            assertFailsWith(TestException::class) {
                fork(
                    { throw TestException() },
                    { 2 + 2 }
                )
            }

            assertFailsWith(TestException::class) {
                fork(
                    { 1 + 1 },
                    { throw TestException() }
                )
            }
        }
    }

    @Test
    fun error() {
        forking {
            assertFailsWith(TestError::class) {
                fork(
                    { throw TestError() },
                    { 2 + 2 }
                )
            }

            assertFailsWith(TestError::class) {
                fork(
                    { 1 + 1 },
                    { throw TestError() }
                )
            }
        }
    }
}
