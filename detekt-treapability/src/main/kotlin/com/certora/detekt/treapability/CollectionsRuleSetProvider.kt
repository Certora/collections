package com.certora.detekt.treapability

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class CollectionsRuleSetProvider : RuleSetProvider {

    override val ruleSetId: String = "certora-collections"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(Treapability(config))
    )
}
