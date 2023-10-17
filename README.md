# Certora Collections

Efficient functional collections for Kotlin.

TODO: need a better name

Certora Collections provides data structure implementations for Kotlin that are designed for efficient functional programming.
These data structures are immutable and persistent, and designed for minimal memory usage when used in a functional
style. They also include other features that we have found helpful in implementing the Certora Prover, such as efficient
joining/merging of maps, parallel operations, etc.

We build on the interfaces introduced in
[kotlinx.collections.immutable](https://github.com/Kotlin/kotlinx.collections.immutable), but provide a different
underlying implementation based on "[treaps](https://en.wikipedia.org/wiki/Treap)," which are probabilistically balanced
binary search trees (BSTs).

Currently we provide set and map implementations.

# Motivation





TODO: describe why we use treaps, what are the benefits, etc.


## Usage

...how to get the package in Gradle

## Docs
