package com.fajrbahr.mediatork.sample.ktor.after.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.ktor.after.data.cache.AladhanCacheDataSource

fun appRegistrar(cache: AladhanCacheDataSource): MediatorRegistrar = object : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.register(getPrayerTimesHandler(cache))
        registry.register(getIslamicMonthsHandler(cache))
    }
}
