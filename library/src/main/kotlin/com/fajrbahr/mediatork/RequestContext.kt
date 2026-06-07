package com.fajrbahr.mediatork

class RequestContext {
    private val metaDate = mutableMapOf<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getMetaDate(key: String): T? = metaDate[key] as? T
    fun put(key: String, value: Any?) {
        metaDate[key] = value
    }

}
