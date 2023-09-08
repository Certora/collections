package com.certora.detekt.treapability

import io.gitlab.arturbosch.detekt.api.*
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.resolve.calls.util.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*

/**
    Enforces hash code stability for types marked [Treapable].  See [Treapable] for the rules.
 */
@RequiresTypeResolution
class Treapability(config: Config) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Defect,
        "Enforces the rules of @$treapableAnnotationName.",
        Debt.TWENTY_MINS
    )

    /**
        If a concrete class implements a "treapable" interface, verify that it does have a stable 'hashCode' 
        implementation.
     */
    override fun visitClassOrObject(clazz: KtClassOrObject) {
        super.visitClassOrObject(clazz)

        val desc = bindingContext.getDescriptor(clazz) ?: return
        when {
            !desc.isConcreteClass -> return
            !desc.isAnnotatedTreapable -> return

            // Enums are restricted to the default [Object.hashCode], which is not stable.
            desc.isEnum -> {
                report(CodeSmell(
                    issue,
                    Entity.from(clazz),
                    "An enum class cannot be treapable, because enums do not have stable hash codes."
                ))
            }

            /*
                - "value classes" get their hashCode from the underlying property.
                - "data classes" have a default hashCode implementation derived from class' properties.
                - In these cases, check that all relevant properties have stable hash code.
             */
            desc.isValue ||
            (desc.isData && desc.findDeclaredHashCode(checkSupers = false) == null) -> {
                val primaryConstructor = desc.unsubstitutedPrimaryConstructor ?: return
                for (prop in primaryConstructor.valueParameters) {
                    checkPropertyAccess(prop, prop.type, desc, prop.kotlinSource ?: clazz)
                }
            }

            // Regular classes need a hashCode implementation, since [Object.hashCode] is not stable.
            else -> {
                val hashCodeFuncDescriptor = desc.findDeclaredHashCode(checkSupers = true) ?: return
                val hashCodeFunc = hashCodeFuncDescriptor.kotlinSource
                if (hashCodeFunc == null) {
                    val message =
                        "'${desc.name}' does not have a stable hash code." +
                        if (desc.isObject) { " Add 'override fun hashCode() = utils.hashObject(this)'." }
                        else { " Add 'override fun hashCode(): Int'." }
                    report(CodeSmell(issue, Entity.from(clazz), message))
                } else {
                    /*
                        We have a hash code implementation.  Check the body for things that look fishy. This is a very
                        simple heuristic: we just look for references to properties that don't have stable hashCode
                        implementation, and flag those.  We make an exception for cases where the value is passed to
                        'utils.HashCode.plus', because we know that does produce a stable hash code. So, we flag this:

                            ```
                            enum class E { A, B, C } // enums don't have stable hash codes
                            class C(i: Int, e: E) {
                                override fun hashCode() = 31 * i + e // Oops, referenced 'e' here!
                            }
                            ```

                        ...but we allow this:

                            ```                     
                            class C(i: Int, e: E) {
                                override fun hashCode() = hash { it + i + e } // e is passed to 'HashCode.plus'
                            }
                            ```
                     */
                    hashCodeFunc.accept(object : DetektVisitor() {
                        override fun visitReferenceExpression(expression: KtReferenceExpression) {
                            super.visitReferenceExpression(expression)
                            val parent = expression.parent as? KtElement ?: return
                            val call = parent.getResolvedCall(bindingContext) ?: return
                            if (call.resultingDescriptor.fqNameSafe != hashCodePlusName) {
                                val prop = bindingContext[BindingContext.REFERENCE_TARGET, expression]
                                    as? PropertyDescriptor ?: return
                                checkPropertyAccess(prop, prop.type, null, expression)
                            }
                        }
                    })
                }
            }
        }
    }

    /**
        Check explicit generic type instantiations.  If a type argument requires a stable hash code, make sure the type
        parameter provides one.
     */
    override fun visitTypeReference(typeReference: KtTypeReference) {
        super.visitTypeReference(typeReference)

        val type = bindingContext[BindingContext.TYPE, typeReference] ?: return
        val typeDescriptor = type.classDescriptor ?: return
        val name = typeDescriptor.fqNameSafe.asString()

        typeDescriptor.typeConstructor.parameters.forEachIndexed { i, param ->
            // The parameters and arguments don't necessarily match - we might be analyzing code that won't compile.
            if (i < type.arguments.size) {
                val projection = type.arguments[i]
                if (!projection.isStarProjection) {
                    checkTypeArgument(type.arguments[i].type, param, name, typeReference)
                }
            }
        }
    }

    /**
        Check generic function invocation.  This includes generic class constructors.
     */
    override fun visitExpression(expression: KtExpression) {
        super.visitExpression(expression)

        val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
        // Only check the call expression itself, not any subexpressions.  We have to walk through the ResolvedCall data
        // structure, which wraps a lower-level "Call" class, to find the actual call source element.
        if (resolvedCall.call.callElement == expression) {
            val name = resolvedCall.resultingDescriptor.fqNameSafe.asString()
            for ((param, arg) in resolvedCall.typeArguments) {
                checkTypeArgument(arg, param, name, expression)
            }
        }
    }


    /**
        Check that a property has a stable hash code.  If not, try to give a useful error message.
     */
    private fun checkPropertyAccess(prop: DeclarationDescriptor, propType: KotlinType, usageClass: ClassDescriptor?, loc: KtElement) {
        val unstableType = findTypeWithUnstableHashCode(propType) ?: return
        val message =
            if (unstableType.isEnum) {
                "Property '${prop.name}' does not have a stable hash code, because ${unstableType.displayName} is an Enum type." +
                if (usageClass == null) {
                    ""
                } else {
                    " '${usageClass.fqNameSafe}' should have 'override fun hashCode(): Int'."
                }
            } else {
                "Property '${prop.name}' may not have a stable hash code. " + howToMakeSafe(unstableType, false)
            }
        report(CodeSmell(issue, Entity.from(loc), message))
    }

    /*
        Check that the type argument is compatible with the type parameter.  If not, try to give a useful error message.
     */
    private fun checkTypeArgument(arg: KotlinType, param: TypeParameterDescriptor, declName: String, loc: KtElement) {
        if (param.hasStableHashCodeAnnotation && arg.isPossiblySerializable) {
            val unstableType = findTypeWithUnstableHashCode(arg) ?: return
            val message =
                "Type parameter '${param.name}' of '$declName' requires a stable hash code for potentially serializable types. " +
                howToMakeSafe(unstableType, true)
            report(CodeSmell(issue, Entity.from(loc), message))
        }
    }

    private fun howToMakeSafe(type: KotlinType, onlyIfSerializable: Boolean) = when {
        type.isEnum ->
            "Enum class '${type.displayName}' cannot have stable hash codes."
        type.isTypeParameter() ->
            "'${type}' should have @$treapableAnnotationName."
        !onlyIfSerializable || type.isJavaSerializableClass ->
            "'${type.displayName}' should have @$treapableAnnotationName."
        else ->
            "'${type.displayName}' should be sealed or final, or should have @$treapableAnnotationName."
    }

    companion object {
        private val hashCodePlusName = FqName("com.certora.collect.HashCode.plus")
        val treapableAnnotationName = FqName("com.certora.collect.Treapable")

        val Annotated.hasTreapableAnnotation: Boolean get() = 
            annotations.firstOrNull { it.fqName == treapableAnnotationName } != null

        val ClassDescriptor.isAnnotatedTreapable get() = 
            getAllSuperClassifiers().any { it.hasTreapableAnnotation }


        // Known types with stable hash codes
        private val safeTypeNames = setOf(
            FqName("java.math.BigInteger"),
            FqName("kotlin.Boolean"),
            FqName("kotlin.Byte"),
            FqName("kotlin.Char"),
            FqName("kotlin.Int"),
            FqName("kotlin.Long"),
            FqName("kotlin.Nothing"),
            FqName("kotlin.Short"),
            FqName("kotlin.String"),
            FqName("kotlin.UByte"),
            FqName("kotlin.UInt"),
            FqName("kotlin.ULong"),
            FqName("kotlin.UShort"),
        )

        // Known generic types with stable hash codes - if their type parameters have stable hash codes.
        private val safeCollectionTypeNames = setOf(
            FqName("kotlin.Pair"),
            FqName("kotlin.Triple"),
            FqName("kotlin.collections.Collection"),
            FqName("kotlin.collections.List"),
            FqName("kotlin.collections.Map"),
            FqName("kotlin.collections.Map.Entry"),
            FqName("kotlin.collections.Set"),
            FqName("kotlin.collections.MutableCollection"),
            FqName("kotlin.collections.MutableList"),
            FqName("kotlin.collections.MutableMap"),
            FqName("kotlin.collections.MutableMap.Entry"),
            FqName("kotlin.collections.MutableSet"),
            FqName("kotlinx.collections.immutable.PersistentMap"),
            FqName("kotlinx.collections.immutable.PersistentMap.Builder"),
            FqName("kotlinx.collections.immutable.PersistentSet"),
            FqName("kotlinx.collections.immutable.PersistentSet.Builder"),
            FqName("datastructures.PersistentStack"),
            FqName("datastructures.PersistentStack.Builder"),
        )

        private val TypeParameterDescriptor.hasStableHashCodeAnnotation get() =
            annotations.hasAnnotation(treapableAnnotationName)

        /*
            Given a type, which might be generic, check if it has a stable hash code - and return the type that makes it
            unstable, if any.
         */
        private fun findTypeWithUnstableHashCode(type: KotlinType): KotlinType? = when {
            type.isError -> null // Avoid false positives for unresolvable types
            type.isCapturedStarProjection -> null // *-projections are a pass-through - stability is verified elsewhere.
            type.isAnnotatedTypeArgument(treapableAnnotationName) -> null
            else -> type.classDescriptor.let { desc ->
                when {
                    desc == null -> type // this shouldn't happen; make sure it ends up in the output
                    desc.isAnnotatedTreapable -> null
                    desc.fqNameSafe in safeTypeNames -> null
                    desc.fqNameSafe in safeCollectionTypeNames ->
                        // Are all of the type arguments safe?
                        type.arguments.mapNotNull {
                            if (it.isStarProjection) {
                                null // *-projections are a pass-through - stability is verified elsewhere.
                            } else {
                                findTypeWithUnstableHashCode(it.type)
                            }
                        }.firstOrNull()
                    else -> type
                }
            }
        }

        /**
            Might this type be serializable?
         */
        val KotlinType.isPossiblySerializable: Boolean get() = when {
            isError -> false // Avoid false positives for unresolvable types
            else -> classDescriptor.let { desc ->
                when {
                    // Can't find a class?  Assume it's serializable
                    desc == null -> true
                    // It's marked serializable?  Ok.
                    desc.implementsJavaSerializable -> true
                    // Not marked, and final -> no subclasses to add Serializable...
                    desc.isFinalClass -> false
                    // Sealed, and all subclasses are definitely not serializable?
                    desc.isSealed() && desc.sealedSubclasses.none { it.defaultType.isPossiblySerializable } -> false
                    // We don't know if it's serializable, so it's possible
                    else -> true
                }
            }
        }

        /**
            Find 'override fun hashCode()', if there is one, in this class, and maybe its superclasses/interfaces.
         */
        fun ClassDescriptor.findDeclaredHashCode(checkSupers: Boolean): FunctionDescriptor? {
            return findDeclaredFunction("hashCode", checkSupers) { it.valueParameters.isEmpty() && it.typeParameters.isEmpty() }
        }

        /**
            Display the name of a type usefully.
         */
        val KotlinType.displayName: String
            get() = constructor.declarationDescriptor?.fqNameSafe?.asString() ?: "<$this>"

    }
}
