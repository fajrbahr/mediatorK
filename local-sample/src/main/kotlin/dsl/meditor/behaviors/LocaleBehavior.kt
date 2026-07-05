package dsl.meditor.behaviors

import com.fajrbahr.mediatork.api.Stage
import com.fajrbahr.mediatork.feature.behavior
import dsl.meditor.context.locale
import java.util.*

val localeBehavior = behavior(
    stage = Stage.Pre,
    order = -10
) { requestContext, next, request ->
    val systemLocale = Locale.getDefault().language
    requestContext.locale = systemLocale
    next(request)
}
