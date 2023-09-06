package com.certora.detekt.utils

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.source.*

/**
 * Try to find Kotlin source code for this declaration
 */
val DeclarationDescriptorWithSource.kotlinSource get() = source.getPsi() as KtElement?
