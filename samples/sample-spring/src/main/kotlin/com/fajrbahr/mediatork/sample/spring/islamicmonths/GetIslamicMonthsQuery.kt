package com.fajrbahr.mediatork.sample.spring.islamicmonths

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.spring.AladhanCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.springframework.stereotype.Component
import java.net.HttpURLConnection
import java.net.URL

class GetIslamicMonthsQuery : Request<List<IslamicMonth>>

class GetIslamicMonthsHandler(
    private val cache: AladhanCache,
) : RequestHandler<GetIslamicMonthsQuery, List<IslamicMonth>> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetIslamicMonthsQuery,
    ): List<IslamicMonth> {
        cache.getIslamicMonths()?.let { return it }
        return withContext(Dispatchers.IO) {
            parse(fetch("https://api.aladhan.com/v1/islamicMonths"))
                .also { cache.saveIslamicMonths(it) }
        }
    }

    private fun fetch(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept-Encoding", "")
            conn.connect()
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(json: String): List<IslamicMonth> {
        val data = JSONObject(json).getJSONObject("data")
        return (1..12).map { i ->
            val month = data.getJSONObject(i.toString())
            IslamicMonth(
                number = month.getInt("number"),
                nameEn = month.getString("en"),
                nameAr = month.getString("ar"),
            )
        }
    }
}

@Component
class IslamicMonthsRegistrar(private val cache: AladhanCache) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetIslamicMonthsHandler(cache)
    }
}
