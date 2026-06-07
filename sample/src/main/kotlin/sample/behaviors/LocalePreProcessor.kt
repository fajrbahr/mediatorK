package sample.behaviors

import com.fajrbahr.mediatork.Request
import com.fajrbahr.mediatork.RequestContext
import com.fajrbahr.mediatork.RequestPreProcessor
import sample.context.locale
import java.util.*

class LocalePreProcessor : RequestPreProcessor {
    override suspend fun process(
        requestContext: RequestContext,
        request: Request<*>
    ) {
        // Get JVM default locale (e.g., from OS setting)
        val systemLocale = Locale.getDefault().language  // "en", "fr", "de", etc.
        requestContext.locale = systemLocale
    }
}