package com.fajrbahr.mediatork.sample.spring.after.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.spring.after.data.cache.AladhanCacheDataSource
import org.springframework.stereotype.Component

// DSL-based registrar factory
fun appRegistrar(cache: AladhanCacheDataSource): MediatorRegistrar = object : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.register(getPrayerTimesHandler(cache))
        registry.register(getIslamicMonthsHandler(cache))
    }
}

// Spring wrapper for dependency injection
@Component
class AppRegistrar(private val cache: AladhanCacheDataSource) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        appRegistrar(cache).register(registry)
    }
}
