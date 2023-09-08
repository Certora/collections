package com.certora.detekt.treapability

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.inference.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.resolve.source.*
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.*


val ClassDescriptor.implementsJavaSerializable get() = implementsInterface(FqName("java.io.Serializable"))
val KotlinType.isJavaSerializableClass get() = classDescriptor?.implementsJavaSerializable ?: false

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
    If this type represents the argument to a type parameter, look up the declaration of the parameter and see if it has
    the requested annotation.
 */
fun KotlinType.isAnnotatedTypeArgument(annotation: FqName) =
    isTypeParameter() &&
    declarationDescriptor?.annotations?.hasAnnotation(annotation) == true

/**
    Try to find Kotlin source code for this declaration
 */
val DeclarationDescriptorWithSource.kotlinSource get() = source.getPsi() as KtElement?

fun ClassDescriptor.implementsInterface(name: FqName) = getAllSuperClassifiers().any { it.fqNameSafe == name }

val ClassDescriptor.isConcreteClass get() = when (kind) {
    ClassKind.CLASS, ClassKind.OBJECT, ClassKind.ENUM_CLASS -> {
        when (modality) {
            Modality.OPEN, Modality.FINAL -> true
            Modality.SEALED, Modality.ABSTRACT -> false
        }
    }
    else -> false
}

val ClassDescriptor.isEnum get() = kind == ClassKind.ENUM_CLASS
val ClassDescriptor.isObject get() = kind == ClassKind.OBJECT
val ClassDescriptor.isInterface get() = kind == ClassKind.INTERFACE

tailrec fun ClassDescriptor.findDeclaredFunction(
    name: String,
    checkSuperClasses: Boolean,
    filter: (FunctionDescriptor) -> Boolean
): FunctionDescriptor? {
    val func =
        unsubstitutedMemberScope.getContributedFunctions(
            Name.identifier(name),
            NoLookupLocation.WHEN_RESOLVE_DECLARATION
        ).firstOrNull {
            it.containingDeclaration == this && it.kind == CallableMemberDescriptor.Kind.DECLARATION && filter(it)
        }

    return when {
        func != null -> func
        checkSuperClasses -> getSuperClassOrAny().findDeclaredFunction(name, checkSuperClasses, filter)
        else -> null
    }
}


/**
    The names/signatures of all functions/classes/properties exported by a particular package.
 */
data class PackageExports(
    val functionSignatures: Set<String>,
    val classes: Set<String>,
    val properties: Set<String>,
) {
    fun containsEquivalent(desc: DeclarationDescriptor) = when (desc) {
        is FunctionDescriptor -> with(JvmDescriptorMangler(null)) {
            desc.signatureString(compatibleMode = false) in functionSignatures
        }
        is ClassifierDescriptor -> desc.name.asString() in classes
        is PropertyDescriptor -> desc.name.asString() in properties
        else -> false
    }
}

/**
    Finds all exports from a given package (by name).  These are the things you import if you write 'import
    [packageFqName].*

    @param[scopeElement] Any element from the source code. We use this to get access to the module dependencies.
    @param[packageFqName] The full name of the package.
 */
fun BindingContext.getPackageExports(scopeElement: KtElement, packageFqName: String): PackageExports? {

    val module = getContainingModule(scopeElement)
    val pkg = module.getPackage(FqName(packageFqName))

    val functionSignatures = mutableSetOf<String>()
    val classes = mutableSetOf<String>()
    val properties = mutableSetOf<String>()


    with(JvmDescriptorMangler(null)) {
        for (desc in pkg.memberScope.getContributedDescriptors()) {
            if (desc.isExported(compatibleMode = false)) {
                when (desc) {
                    is FunctionDescriptor -> functionSignatures += desc.signatureString(compatibleMode = false)
                    is ClassifierDescriptor -> classes += desc.name.asString()
                    is PropertyDescriptor -> properties += desc.name.asString()
                    else -> error("Unhandled package export $desc")
                }
            }
        }
    }

    return PackageExports(
        functionSignatures = functionSignatures,
        classes = classes,
        properties = properties,
    )
}

/**
    Generates a string representing the full signature of a function
 */
val FunctionDescriptor.jvmSignatureString get() =
    with(JvmDescriptorMangler(null)) {
        signatureString(compatibleMode = false)
    }

/**
    Given an element in the source, get the descriptor of the enclosing declaration, if any.
 */
fun BindingContext.getEnclosingDeclarationDescriptor(element: KtElement) =
    element.parentsWithSelf.map {
        this[BindingContext.DECLARATION_TO_DESCRIPTOR, it]
    }.firstOrNull {
        it != null
    }

/**
    Given an element in the source code, gets the ModuleDescriptor for the Kotlin module containing that code. This
    gives us access to, e.g., the `getPackage` method, so we can look up the package's exports.

    @param[element] Any element from this module's source.
 */
fun BindingContext.getContainingModule(element: KtElement): ModuleDescriptor {
    var desc = getEnclosingDeclarationDescriptor(element)
    while (desc != null) {
        when (desc) {
            is ModuleDescriptor -> return desc
            else -> desc = desc.containingDeclaration
        }
    }
    error("Could not find module for $this")
}

/**
    Gets a ClassDescriptor for a class or object element
 */
fun BindingContext.getDescriptor(classOrObject: KtClassOrObject) =
    this[BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject] as? ClassDescriptor
