package com.certora.detekt.treapability

import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.*
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@KotlinCoreEnvironmentTest
class TreapabilityTest(val env: KotlinCoreEnvironment) {
    val library = """
        package com.certora.collect
        import java.io.Serializable

        @Treapable
        interface StableHashCode

        @Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.CLASS)
        annotation class Treapable

        inline fun hash(initial: Int = 0, action: (TreapableHashCode) -> TreapableHashCode) = action(TreapableHashCode(initial)).code
        @JvmInline value class TreapableHashCode(val code: Int) {
            @PublishedApi
            internal inline fun add(obj: Any?) = TreapableHashCode(31 * this.code + obj.hashCode())

            inline infix operator fun <@Treapable T> plus(obj: T) = add(obj)
            inline infix operator fun plus(clazz: Class<*>?) = add(clazz?.name)
            inline infix operator fun plus(clazz: KClass<*>?) = add(clazz?.java?.name)
            inline infix operator fun plus(e: Enum<*>?) = add(e?.name)
        }

        //
        // Some utility classes for the tests
        //
        class Unstable : Serializable
        class UnstableNonSerializable
        class Stable : StableHashCode, Serializable
        enum class UnstableEnum { A, B }
        open class Container<@Treapable T>(t: T)
        interface IContainer<@Treapable T>
        fun <@Treapable T> genericFunc(t: T) = t
    """

    fun failureCount(code: String) =
        Treapability(TestConfig()).lintWithContext(env, code, library).size

    @Test
    fun dataClassWithPrimitiveTypePasses() {
        val code = """
            import com.certora.collect.*
            data class A(val n: Int) : StableHashCode
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun classWithStableHashCodePasses() {
        val code = """
            import com.certora.collect.*
            class A(val n: Int) : StableHashCode {
                override fun hashCode() = n.hashCode()
            }
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun classWithoutHashCodeFails() {
        val code = """
            import com.certora.collect.*
            class A(val n: Int) : StableHashCode
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun classWithoutHashCodeFailsDirectlyAnnotated() {
        val code = """
            import com.certora.collect.*
            @Treapable
            class A(val n: Int)
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun dataClassWithStableHashCodePasses() {
        val code = """
            import com.certora.collect.*
            data class A(val b: Stable) : StableHashCode
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun enumClassFails() {
        val code = """
            import com.certora.collect.*
            enum class E : StableHashCode {
                ITEM1, ITEM2
            }
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun dataClassWithUnstablePropertyFails() {
        val code = """
            import com.certora.collect.*
            data class AA(val b: Unstable) : StableHashCode
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun dataClassWithEnumPropertyFails() {
        val code = """
            import com.certora.collect.*
            data class AA(val b: UnstableEnum) : StableHashCode
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun valueClassWithUnstablePropertyFails() {
        val code = """
            import com.certora.collect.*
            value class AA(val b: Unstable) : StableHashCode
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun valueClassWithStablePropertyPasses() {
        val code = """
            import com.certora.collect.*
            value class AA(val b: Stable) : StableHashCode
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun classWithBadHashCodeFails() {
        val code = """
            import com.certora.collect.*
            class AA(val b: Unstable) : StableHashCode {
                override fun hashCode() = b.hashCode()
            }
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun classWithGoodHashCodePasses() {
        // Generic plus calls hashCode(), so an unstable serializable property is still a violation.
        // The documented safe form is the enum overload, which hashes the enum name.
        // See documentedTreapHashPlusIsAccepted.
        val code = """
            import com.certora.collect.*
            class AA(val b: Unstable) : StableHashCode {
                override fun hashCode() = hash { it + b }
            }
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun documentedTreapHashPlusIsAccepted() {
        val code = """
            import com.certora.collect.*
            class AA(val e: UnstableEnum) : StableHashCode {
                override fun hashCode() = hash { it + e }
            }
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun explicitTreapHashPlusCallIsAccepted() {
        val code = """
            import com.certora.collect.*
            class AA(val e: UnstableEnum) : StableHashCode {
                override fun hashCode() = hash { it.plus(e) }
            }
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun parenthesizedTreapHashPlusOperandIsAccepted() {
        val code = """
            import com.certora.collect.*
            class AA(val e: UnstableEnum) : StableHashCode {
                override fun hashCode() = hash { it + (e) }
            }
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun propertyPassedToOtherFunctionFails() {
        val code = """
            import com.certora.collect.*
            class AA(val e: UnstableEnum) : StableHashCode {
                override fun hashCode() = java.util.Objects.hash(e)
            }
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun hashCodeReturningPropertyDirectlyFails() {
        val code = """
            import com.certora.collect.*
            class AA(val b: Unstable) : StableHashCode {
                override fun hashCode(): Int = b
            }
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun derivedFromGoodContainerPasses() {
        val code = """
            import com.certora.collect.*
            class AA() : Container<Int>(12)
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun derivedFromGoodContainerInterfacePasses() {
        val code = """
            import com.certora.collect.*
            class AA() : IContainer<Int>
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun derivedFromBadContainerFails() {
        val code = """
            import com.certora.collect.*
            class AA() : Container<Unstable>(Unstable())
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun derivedFromBadContainerInterfaceFails() {
        val code = """
            import com.certora.collect.*
            class AA() : IContainer<UnstableEnum>
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun callBadFuncFails() {
        val code = """
            import com.certora.collect.*
            val f = genericFunc(UnstableEnum.A)
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun callGoodFuncPasses() {
        val code = """
            import com.certora.collect.*
            val f = genericFunc(Stable())
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun callBadConstructorFails() {
        val code = """
            import com.certora.collect.*
            val f = Container(UnstableEnum.A)
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun callGoodConstructorPasses() {
        val code = """
            import com.certora.collect.*
            val f = Container(Stable())
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun callGoodConstructorNonSerializablePasses() {
        val code = """
            import com.certora.collect.*
            val f = Container(UnstableNonSerializable())
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun callGoodFuncNonSerializablePasses() {
        val code = """
            import com.certora.collect.*
            val f = genericFunc(UnstableNonSerializable())
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun listOfStableElementsPasses() {
        val code = """
            import com.certora.collect.*
            val f = genericFunc(listOf(1, 2))
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun listOfUnstableElementsFails() {
        val code = """
            import com.certora.collect.*
            val f = genericFunc(listOf(Unstable()))
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun starProjectionIsNotCheckedHere() {
        val code = """
            import com.certora.collect.*
            fun f(c: Container<*>) = c
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun sealedNonSerializableArgumentPasses() {
        val code = """
            import com.certora.collect.*
            sealed class S
            class C : S()
            fun use(s: S) = genericFunc(s)
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun dataClassWithExplicitHashCodePasses() {
        val code = """
            import com.certora.collect.*
            data class A(val b: Unstable) : StableHashCode {
                override fun hashCode() = 1
            }
        """
        assertEquals(0, failureCount(code))
    }

    @Test
    fun objectWithoutHashCodeFails() {
        val code = """
            import com.certora.collect.*
            object O : StableHashCode
        """
        assertEquals(1, failureCount(code))
    }

    @Test
    fun objectFixHintNamesTheRealHelper() {
        val code = """
            import com.certora.collect.*
            object O : StableHashCode
        """
        val findings = Treapability(TestConfig()).lintWithContext(env, code, library)
        assertEquals(1, findings.size)
        val message = findings.single().message
        assertTrue(message.contains("treapHashObject(this)"))
        assertFalse(message.contains("utils.treapHashObject"))
    }

    @Test
    fun providerRegistersTreapability() {
        val provider = CollectionsRuleSetProvider()
        assertEquals("certora-collections", provider.ruleSetId)
        val rules = provider.instance(TestConfig()).rules
        assertEquals(1, rules.size)
        assertTrue(rules.single() is Treapability)
    }
}
