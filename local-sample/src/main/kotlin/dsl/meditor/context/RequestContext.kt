package dsl.meditor.context

import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.contextKey

val LocaleKey = contextKey<String>("locale")
var RequestContext.locale: String
    get() = this[LocaleKey] ?: "en"
    set(value) {
        this[LocaleKey] = value
    }

