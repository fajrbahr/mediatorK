package com.fajrbahr.mediatork

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.RequestContext

interface HandlerScope : Mediator {
    val context: RequestContext
}
