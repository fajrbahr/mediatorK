package sample.meditor.context

import com.fajrbahr.mediatork.RequestContext

var RequestContext.locale: String
    get() = getMetaDate("locale") ?: "en"
    set(value) {
        put("locale", value)
    }
