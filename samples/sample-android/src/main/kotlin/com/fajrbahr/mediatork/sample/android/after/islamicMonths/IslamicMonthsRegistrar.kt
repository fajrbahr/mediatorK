package com.fajrbahr.mediatork.sample.android.after.islamicMonths

import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource

fun islamicMonthsRegistrar(cache: AladhanCacheDataSource): MediatorRegistrar = object : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.register(getIslamicMonthsHandler(cache))
    }
}
