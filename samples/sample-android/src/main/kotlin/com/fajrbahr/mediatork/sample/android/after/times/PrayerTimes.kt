package com.fajrbahr.mediatork.sample.android.after.times

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
) {
    val prayers: List<PrayerTime> = listOf(fajr, sunrise, dhuhr, asr, maghrib, isha)
}
