package com.fajrbahr.mediatork.api

/**
 * Type-safe key for storing and retrieving context values in [RequestContext].
 * Prevents typos and casting errors compared to string-based API.
 */
interface ContextKey<T> {
    val name: String
}

fun <T> contextKey(name: String): ContextKey<T> = object : ContextKey<T> {
    override val name = name
}
