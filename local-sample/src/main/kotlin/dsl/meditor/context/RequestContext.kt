package dsl.meditor.context

import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.contextKey

/** Type-safe context key for locale/language preference. */
val LocaleKey = contextKey<String>("locale")

/** Type-safe locale convenience extension. */
var RequestContext.locale: String
    get() = this[LocaleKey] ?: "en"
    set(value) {
        this[LocaleKey] = value
    }

