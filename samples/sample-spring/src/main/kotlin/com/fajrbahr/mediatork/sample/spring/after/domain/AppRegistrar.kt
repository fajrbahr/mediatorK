package com.fajrbahr.mediatork.sample.spring.after.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.spring.after.data.cache.AladhanCacheDataSource
import org.springframework.stereotype.Component

@Component
class AppRegistrar(private val cache: AladhanCacheDataSource) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.register(GetPrayerTimesHandler(cache))
        registry.register(GetIslamicMonthsHandler(cache))
    }
}
