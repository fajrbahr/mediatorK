package com.fajrbahr.mediatork.api

/**
 * Mutable key-value bag scoped to a single request pipeline execution.
 *
 * A fresh [RequestContext] is created for every [com.fajrbahr.mediatork.handler.Sender.send] call, so values
 * stored here are never shared between concurrent requests — even when the
 * mediator itself is a singleton. This design mirrors the per-request scoping of
 * `HttpContext` in web frameworks.
 *
 * Pipeline behaviors and pre-processors populate the context; handlers and
 * post-processors consume the values. Keys are plain strings; callers are
 * responsible for avoiding key collisions (e.g. by using fully qualified names
 * or companion-object constants).
 */
class RequestContext {
    /** Internal storage for arbitrary metadata keyed by name. */
    private val metadata = mutableMapOf<String, Any?>()

    /**
     * Retrieves a value from the context, casting it to [T].
     *
     * Returns `null` if the key is absent or the stored value cannot be cast to [T].
     *
     * @param T the expected type of the stored value.
     * @param key the key under which the value was stored.
     * @return the cast value, or `null` if the key is absent or the cast fails.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getMetaDate(key: String): T? = metadata[key] as? T

    /**
     * Stores [value] under [key], replacing any previously stored value.
     *
     * @param key the key to associate with [value].
     * @param value the value to store; may be `null`.
     */
    fun put(key: String, value: Any?) {
        metadata[key] = value
    }
}
