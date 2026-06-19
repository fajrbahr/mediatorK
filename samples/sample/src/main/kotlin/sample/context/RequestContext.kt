package sample.context

import com.fajrbahr.mediatork.api.RequestContext

data class CurrentUser(
    val id: String,
    val name: String,
    val email: String,
    val token: String,
    val roles: List<String>
)

var RequestContext.currentUser: CurrentUser?
    get() = getMetaDate("currentUser")
    set(value) {
        put("currentUser", value)
    }

var RequestContext.locale: String
    get() = getMetaDate("locale") ?: "en"
    set(value) {
        put("locale", value)
    }

data class TraceMeta(val name: String, val value: Long)

fun RequestContext.addTraceMeta(metrics: List<Pair<String, Long>>) {
    val metrics = metrics.map { TraceMeta(it.first, it.second) }
    put("traceMeta", metrics)
}

val RequestContext.traceNetworkMetrics: List<TraceMeta>
    get() = getMetaDate("traceMeta") ?: emptyList()
