package com.fajrbahr.mediatork.sample.ktor.islamicmonths

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.MediatorBuilder
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.sample.ktor.AladhanCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GetIslamicMonthsQuery : Request<List<IslamicMonth>>

fun getIslamicMonthsHandler(cache: AladhanCache): Handler<GetIslamicMonthsQuery, List<IslamicMonth>> = {
    cache.getIslamicMonths() ?: withContext(Dispatchers.IO) {
        parseIslamicMonths(fetchIslamicMonths("https://api.aladhan.com/v1/islamicMonths"))
            .also { cache.saveIslamicMonths(it) }
    }
}

fun MediatorBuilder.islamicMonthsModule(cache: AladhanCache) {
    handle(getIslamicMonthsHandler(cache))
}

private fun fetchIslamicMonths(url: String): String {
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

private fun parseIslamicMonths(json: String): List<IslamicMonth> {
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
