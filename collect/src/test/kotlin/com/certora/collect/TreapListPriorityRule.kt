package com.certora.collect

import kotlin.random.Random
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
    Installs a fresh [Random] for list priorities around each test method.

    The generator is stored on the running thread, so test order does not matter and two test threads do not share
    draws. Any test that builds a [TreapList] needs this rule. Without it, that test still draws from
    [Random.Default].
 */
class TreapListPriorityRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            TreapListNode.setTreapListPriorityRandom(Random(TREAP_LIST_PRIORITY_SEED))
            try {
                base.evaluate()
            } finally {
                TreapListNode.setTreapListPriorityRandom(null)
            }
        }
    }

    internal companion object {
        const val TREAP_LIST_PRIORITY_SEED = 1
    }
}
