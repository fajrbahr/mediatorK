package com.opentool.mediatork.com.opentool.mediatork

class RequestContext {
    private val metaDate = mutableMapOf<String, Any?>()

    @Suppress("UNCHECKED_CAST")
    fun <T> getMetaDate(key: String): T? = metaDate[key] as? T
    fun put(key: String, value: Any?) {
        metaDate[key] = value
    }

}

var RequestContext.locale: String
    get() = getMetaDate("locale") ?: "en"
    set(value) {
        put("locale", value)
    }

data class TraceMeta(val name: String, val value: Long)

fun RequestContext.addTraceMeta(metrics: List<Pair<String, Long>>) {
   val metrics =  metrics.map {  TraceMeta(it.first, it.second)}
        put("traceMeta", metrics)
}

val RequestContext.traceNetworkMetrics: List<TraceMeta>
    get() = getMetaDate("traceMeta") ?: emptyList()
