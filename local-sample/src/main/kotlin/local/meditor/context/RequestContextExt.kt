package local.meditor.context

import com.fajrbahr.mediatork.RequestContext

var RequestContext.locale: String
    get() = getMetaData("locale") ?: "en"
    set(value) {
        put("locale", value)
    }
