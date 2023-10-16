package com.certora.collect

/** 
    Marker interface to indicate that a type should be stored in a hashed treap, even if it is [Comparable].  Use this
    on types whose [Comparable] implementation is not suitable.
*/
public interface PrefersHashTreap