package com.certora.common.collect

/**
    When applied to a class or interface, indicates that the class or interface has a stable hash code.  When applied to
    a generic type parameter, indicates that arguments to the type parameter must have stable hash codes if they might
    be serialized.

    The internal structure of Treap-based collections is based on the hash codes of the keys stored in the collection.
    If a Treap is serialized and deserialized, the hash codes of the keys must be the same as they were before
    serialization, or the Treap will be corrupted.  To prevent this, we use [Treapable] annotations to convey hash code
    stability requirements, and optionally enforce these requirements with the [Treapability] Detekt rule. 

    A type parameter annotated with [Treapable] requires that all arguments to the type parameter have stable hash code
    *if* they are also possibly serializable.  

    A type is definitely not serializable if it does not implement Serializable *and* it is final (or sealed, and all
    subclasses are definitely not serializable).

    A class, interface, or object annotated with [Treapable] must have a stable hash code (and all subclasses must
    also).  The [Treapability] rule will report a violation if a class, interface, or object annotated with [Treapable]
    does not appear to have a stable hash code.  

    The [Treapability] rule will also report a violation if an argument to a [Treapable] type parameter is potentially
    serializable and does not have a stable hash code.  Some notes on hash code stability:

    - The default [Object.hashCode] implementation does not produce a stable hash code; it effecitvely assigns a random
      number to each class instance.  A "naked" class that does not override hashCode will not have a stable hash code.

    - Kotlin/JVM primitive types (Int, Char, etc.) do have stable hash codes.

    - Kotlin/JVM Strings have stable hash codes.

    - Kotlin "data classes" provide a hashCode implementation which is stable as long as the properties in the data
      class' primary constructor all have stable hash codes.

    - Kotlin "object" types do not provide stable hash codes by default; they get the random number from the underlying
      [Object.hashCode] implementation.

    - Classes which implement [Collection] or [Map] are required to have stable hash codes (as part of the semantics of
      those interfaces), so long as the elements stored in those collections also have stable hash codes.

    - Kotlin/JVM "enum classes" do not have stable hash codes.  They use the default [Object.hashCode] implementation,
      and do not allow this to be overridden.  A stable hash code can be obtained from the *name* of the enum instance.
      (We could also use the ordinal, but this has an unnecessary dependence on ordering.)

    - Our analysis considers some known JVM types to have stable hash codes (such as BigIngeter).
 */
@Target(AnnotationTarget.TYPE_PARAMETER, AnnotationTarget.TYPE, AnnotationTarget.CLASS)
public annotation class Treapable
