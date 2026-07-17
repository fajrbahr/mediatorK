package local.meditor.behaviors

import com.fajrbahr.mediatork.Behavior
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.behavior
import local.meditor.context.locale
import java.util.Locale

fun localeBehavior(): Behavior = behavior(order = -10) { _, context , next ->
    context.locale = Locale.getDefault().language
    next()
}
