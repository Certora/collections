package com.certora.detekt.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmDescriptorMangler
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * The names/signatures of all functions/classes/properties exported by a particular package.
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
 * Finds all exports from a given package (by name).  These are the things you import if you write
 * 'import [packageFqName].*
 *
 * @param[scopeElement] Any element from the source code. We use this to get access to the module dependencies.
 * @param[packageFqName] The full name of the package.
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
 * Generates a string representing the full signature of a function
 */
val FunctionDescriptor.jvmSignatureString get() =
    with(JvmDescriptorMangler(null)) {
        signatureString(compatibleMode = false)
    }

/**
 * Given an element in the source, get the descriptor of the enclosing declaration, if any.
 */
fun BindingContext.getEnclosingDeclarationDescriptor(element: KtElement) =
    element.parentsWithSelf.map {
        this[BindingContext.DECLARATION_TO_DESCRIPTOR, it]
    }.firstOrNull {
        it != null
    }

/**
 * Given an element in the source code, gets the ModuleDescriptor for the Kotlin module containing that code.
 * This gives us access to, e.g., the `getPackage` method, so we can look up the package's exports.
 *
 * @param[element] Any element from this module's source.
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
 * Gets a ClassDescriptor for a class or object element
 */
fun BindingContext.getDescriptor(classOrObject: KtClassOrObject) =
    this[BindingContext.DECLARATION_TO_DESCRIPTOR, classOrObject] as? ClassDescriptor
