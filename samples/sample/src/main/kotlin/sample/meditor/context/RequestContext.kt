package sample.meditor.context

import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.meta

var RequestContext.locale: String by meta("locale", default = "en")
