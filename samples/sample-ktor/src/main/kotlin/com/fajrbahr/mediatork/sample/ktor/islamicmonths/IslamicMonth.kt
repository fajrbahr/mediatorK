package com.fajrbahr.mediatork.sample.ktor.islamicmonths

import kotlinx.serialization.Serializable

@Serializable
data class IslamicMonth(
    val number: Int,
    val nameEn: String,
    val nameAr: String,
)
