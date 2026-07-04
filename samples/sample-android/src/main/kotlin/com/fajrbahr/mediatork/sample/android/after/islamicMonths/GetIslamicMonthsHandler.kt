package com.fajrbahr.mediatork.sample.android.after.islamicMonths

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.handler.handler
import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GetIslamicMonthsRequest : Request<List<IslamicMonth>>

fun getIslamicMonthsHandler(cache: AladhanCacheDataSource) =
    handler<GetIslamicMonthsRequest, List<IslamicMonth>> { request ->
        cache.getIslamicMonths()?.let { return@handler it }
        withContext(Dispatchers.IO) {
            parse(fetch("https://api.aladhan.com/v1/islamicMonths"))
                .also { cache.saveIslamicMonths(it) }
        }
    }

private fun fetch(urlString: String): String {
    val connection = URL(urlString).openConnection() as HttpURLConnection
    return try {
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept-Encoding", "")
        connection.connect()
        connection.inputStream.bufferedReader().readText()
    } finally {
        connection.disconnect()
    }
}

private fun parse(json: String): List<IslamicMonth> {
    val data = JSONObject(json).getJSONObject("data")
    return (1..12).map { i ->
        val month = data.getJSONObject(i.toString())
        IslamicMonth(
            number = month.getInt("number"),
            nameEn = month.getString("en"),
            nameAr = month.getString("ar")
        )
    }
}
