package com.fajrbahr.mediatork.sample.android.after.domain

import com.fajrbahr.mediatork.MediatorRegistrar
import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.sample.android.after.data.cache.AladhanCacheDataSource

class IslamicMonthsRegistrar(
    private val cache: AladhanCacheDataSource,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.register(GetIslamicMonthsHandler(cache))
    }
}
