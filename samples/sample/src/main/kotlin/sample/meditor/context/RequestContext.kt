package sample.meditor.context

import com.fajrbahr.mediatork.api.RequestContext

var RequestContext.locale: String
    get() = getMetaData("locale") ?: "en"
    set(value) {
        put("locale", value)
    }
