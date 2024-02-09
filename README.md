# Certora Collections

[![](https://jitpack.io/v/certora/collections.svg)](https://jitpack.io/#certora/collections)

Fast, memory-efficient immutable collections for Kotlin.

## Motivation

Kotlin makes it quite easy and natural to manipulate immutable data structures. However, the standard library does not
provide efficient implementations of immutable collections.
[kotlinx.collections.immutable](https://github.com/Kotlin/kotlinx.collections.immutable) provides a set of interfaces
that are designed to be implemented by efficient immutable collections, along with a reference implementation. However,
in developing the Certora Prover, we found that the reference implementation did not make the right performance
tradeoffs for our use cases.

Most collection implementations, including the ones mentioned previously, are optimized primarily for speed of
operations on single elements of the collection, e.g., adding an element to a `Set`, looking up a single value in a
`Map`, getting a single `List` element by index. However, in many use cases the more performance-critical operations are
those that operate over the whole data structure, such computing set unions or intersection of two sets, or merging two
maps.

The Certora Collections library provides `Set` and `Map` and `List` implementations which are optimized primarily for
such operations.  Further, we optimize heavily for memory usage over speed of operations on single elements.  We also
provide some additional features that we have found useful in the Certora Prover, such as efficient parallel operations.

## Usage

[![](https://jitpack.io/v/certora/collections.svg)](https://jitpack.io/#certora/collections)<br/>
The Certora Collections library is available on [JitPack](https://jitpack.io/#certora/collections).

### API

The API builds on the interfaces provided by [kotlinx.collections.immutable].  See The Kotlin Immutable Collections
[proposal](https://github.com/Kotlin/kotlinx.collections.immutable/blob/master/proposal.md).  We provide some additional
methods beyond the `PersistentMap`, `PersistentSet`, and `PersistentList` interfaces provided by
`kotlinx.collections.immutable`, in the `TreapMap`, `TreapSet`, and `TreapList` interfaces.

To create instances of these collections, use the functions `treapMapOf`, `treapSetOf`, and `treapListOf`.
