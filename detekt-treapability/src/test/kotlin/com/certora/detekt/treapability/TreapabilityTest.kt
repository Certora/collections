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
        val code = """
            import com.certora.collect.*
            class AA(val b: Unstable) : StableHashCode {
                override fun hashCode() = hash { it + b }
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
}
