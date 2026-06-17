package com.fajrbahr.mediatork.sample.ktor.after.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.MediatorRegistrar
import com.fajrbahr.mediatork.sample.ktor.after.data.cache.AladhanCacheDataSource

class AppRegistrar(private val cache: AladhanCacheDataSource) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.register(GetPrayerTimesHandler(cache))
        registry.register(GetIslamicMonthsHandler(cache))
    }
}
