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
    private val metadata = mutableMapOf<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: ContextKey<T>): T? = metadata[key.name] as? T

    operator fun <T> set(key: ContextKey<T>, value: T?) {
        metadata[key.name] = value
    }

    @Suppress("UNCHECKED_CAST")
    fun <T> getMetaData(key: String): T? = metadata[key] as? T

    fun put(key: String, value: Any?) {
        metadata[key] = value
    }
}
