package sample.meditor.behaviors

import com.fajrbahr.mediatork.Behavior
import com.fajrbahr.mediatork.behavior
import sample.meditor.context.locale
import java.util.Locale

/** Pre-handler behavior: stamps the request context with the system locale. */
fun localeBehavior(): Behavior = behavior(order = -10) { _, context, next ->
    context.locale = Locale.getDefault().language
    next()
}
