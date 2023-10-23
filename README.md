# Certora Collections

Memory-efficient immutable collections for Kotlin.

## Motivation

Kotlin makes it quite easy and natural to manipulate immutable data structures. However, the standard library does not
provide efficient implementations of immutable collections.
[kotlinx.collections.immutable](https://github.com/Kotlin/kotlinx.collections.immutable) provides a set of interfaces
that are designed to be implemented by efficient immutable collections, along with a reference implementation. However,
in developing the Certora Prover, we found that the reference implementation did not make the right performance
tradeoffs for our use cases.

Most `Set` and `Map` implementations, including the ones mentioned previously, are optimized primarily for speed of
operations on single elements of the collection, e.g., adding an element to a `Set` or looking up a single value in a
`Map`. However, in many use cases the more performance-critical operations are those that operate over the whole data
structure, such computing set unions or intersection of two sets, or merging two maps.

The Certora Collections library provides `Set` and `Map` implementations which are optimized primarily for such
operations.  Further, we optimize heavily for memory usage over speed of operations on single elements.  We also provide
some additional features that we have found useful in the Certora Prover, such as efficient parallel operations.
