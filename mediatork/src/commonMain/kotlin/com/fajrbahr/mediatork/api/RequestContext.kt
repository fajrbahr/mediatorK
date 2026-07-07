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

class RequestContext {
    private val metadata = mutableMapOf<String, Any?>()

    /**
     * Retrieves a value from the context using a [ContextKey].
     *
     * Returns `null` if the key is absent or the stored value cannot be cast to [T].
     *
     * @param T the expected type of the stored value.
     * @param key the type-safe key under which the value was stored.
     * @return the cast value, or `null` if the key is absent or the cast fails.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getMetaData(key: ContextKey<T>): T? = metadata[key.name] as? T

    /**
     * Retrieves a value from the context using a string key.
     *
     * Returns `null` if the key is absent or the stored value cannot be cast to [T].
     *
     * @param T the expected type of the stored value.
     * @param key the key under which the value was stored.
     * @return the cast value, or `null` if the key is absent or the cast fails.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getMetaData(key: String): T? = metadata[key] as? T

    /**
     * Stores [value] under a [ContextKey], replacing any previously stored value.
     *
     * @param T the type of the value.
     * @param key the type-safe key to associate with [value].
     * @param value the value to store; may be `null`.
     */
    fun <T> put(key: ContextKey<T>, value: T?) {
        metadata[key.name] = value
    }

    /**
     * Stores [value] under a string key, replacing any previously stored value.
     *
     * @param key the key to associate with [value].
     * @param value the value to store; may be `null`.
     */
    fun put(key: String, value: Any?) {
        metadata[key] = value
    }

}
