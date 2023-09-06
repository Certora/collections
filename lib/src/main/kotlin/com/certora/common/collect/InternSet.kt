package com.certora.common.collect

interface InternSet<E> : Set<E> {
    /**
     * Returns an existing element that compares equal to E, or null.
     * This is useful for, e.g., building an intern table.
     */
    fun findEqual(element: E): E?
}