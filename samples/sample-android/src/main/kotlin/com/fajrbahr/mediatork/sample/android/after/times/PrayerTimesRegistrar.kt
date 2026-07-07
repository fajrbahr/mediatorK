package com.fajrbahr.mediatork.sample.android.after.times

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource

class PrayerTimesRegistrar(
    private val cache: AladhanCacheDataSource,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.register(GetPrayerTimesHandler(cache))
    }
}
