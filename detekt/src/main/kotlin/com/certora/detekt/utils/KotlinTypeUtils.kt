package com.certora.detekt.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*

// Note: in the below definitions, "constructor" refers to a "type constructor" from the POV of the Kotlin compiler's
// type system, not a class constructor from Kotlin source code.

val KotlinType.declarationDescriptor get() = constructor.declarationDescriptor
val KotlinType.classDescriptor get() = declarationDescriptor as? ClassDescriptor
val KotlinType.isEnum get() = classDescriptor?.isEnum ?: false

val KotlinType.isCapturedStarProjection get() = when (val c = constructor) {
    is CapturedTypeConstructor -> c.projection.isStarProjection
    else -> false
}

fun KotlinType.hasSupertype(supertype: FqName) =
    supertypes().any { it.classDescriptor?.fqNameSafe == supertype }

/**
 * If this type represents the argument to a type parameter, look up the declaration of the parameter and see if it has
 * the requested annotation.
 */
fun KotlinType.isAnnotatedTypeArgument(annotation: FqName) =
    isTypeParameter() &&
    declarationDescriptor?.annotations?.hasAnnotation(annotation) == true
