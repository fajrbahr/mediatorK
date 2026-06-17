package com.fajrbahr.mediatork.sample.spring.after.model

data class PrayerTime(val name: String, val time: String)

data class TodayPrayerTimes(
    val gregorianDate: String,
    val hijriDate: String,
    val fajr: PrayerTime,
    val sunrise: PrayerTime,
    val dhuhr: PrayerTime,
    val asr: PrayerTime,
    val maghrib: PrayerTime,
    val isha: PrayerTime,
)
