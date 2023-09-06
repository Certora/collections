package com.certora.detekt.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*

val serializationAnnotations = setOf(
    FqName("kotlinx.serialization.Serializable"),
    FqName("kotlinx.serialization.Contextual"),
    FqName("kotlinx.serialization.Transient"),
    FqName("utils.KSerializable"),
    FqName("utils.KTransient"),
)

fun Annotations.getSerializableAnnotation(): AnnotationDescriptor? = firstOrNull { it.fqName in serializationAnnotations }
val ClassDescriptor.hasSerializableAnnotation get() = annotations.getSerializableAnnotation() != null
val Annotated.hasSerializableAnnotation get() = annotations.getSerializableAnnotation() != null

val ClassDescriptor.implementsHasKSerializable get() = implementsInterface(FqName("utils.HasKSerializable"))

val ClassDescriptor.needsSerializableAnnotation get() = when(kind) {
    ClassKind.INTERFACE -> false // @Serializable doesn't apply to interfaces
    ClassKind.ANNOTATION_CLASS -> false // @Serializable doesn't apply to annotations
    ClassKind.ENUM_CLASS -> false // Enums are always serializable (they are serialized by name)
    ClassKind.ENUM_ENTRY -> false // ibid
    else -> implementsHasKSerializable
}

val ClassDescriptor.implementsJavaSerializable get() = implementsInterface(FqName("java.io.Serializable"))
val KotlinType.isJavaSerializableClass get() = classDescriptor?.implementsJavaSerializable ?: false
