package dsl.meditor.behaviors

import com.fajrbahr.mediatork.feature.behavior
import dsl.meditor.context.LocaleKey
import java.util.*

val localeBehavior = behavior(
    order = -10
) { requestContext, next, request ->
    val systemLocale = Locale.getDefault().language
    requestContext[LocaleKey] = systemLocale
    next(request)
}
