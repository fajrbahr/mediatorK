package com.fajrbahr.mediatork.api

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface ContextKey<T> {
    val name: String
}

fun <T> contextKey(name: String): ContextKey<T> = object : ContextKey<T> {
    override val name = name
}

class RequestContext {
    private val metadata = mutableMapOf<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getMetaData(key: ContextKey<T>): T? = metadata[key.name] as? T

    @Suppress("UNCHECKED_CAST")
    fun <T> getMetaData(key: String): T? = metadata[key] as? T

    fun <T> put(key: ContextKey<T>, value: T?) {
        metadata[key.name] = value
    }

    fun put(key: String, value: Any?) {
        metadata[key] = value
    }
}

/**
 * Property delegate for a typed [RequestContext] metadata entry.
 *
 * The metadata store is `Any?`-backed, so no string encode/decode is needed —
 * the value round-trips through [RequestContext.getMetaData]/[RequestContext.put]
 * as-is, and [default] is returned when the key is absent.
 *
 * ```
 * var RequestContext.locale: String by meta("locale", default = "en")
 * ```
 */
fun <T> metaContext(key: String, default: T): ReadWriteProperty<RequestContext, T> =
    object : ReadWriteProperty<RequestContext, T> {
        override fun getValue(thisRef: RequestContext, property: KProperty<*>): T =
            thisRef.getMetaData<T>(key) ?: default

        override fun setValue(thisRef: RequestContext, property: KProperty<*>, value: T) {
            thisRef.put(key, value)
        }
    }
