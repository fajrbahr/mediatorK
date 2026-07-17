package local.meditor.context

import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.metaContext

var RequestContext.locale: String by metaContext("locale", default = "en")
