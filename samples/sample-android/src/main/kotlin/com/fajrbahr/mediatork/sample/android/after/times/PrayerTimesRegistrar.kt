package com.fajrbahr.mediatork.sample.android.after.times

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.android.after.AladhanCacheDataSource

fun prayerTimesRegistrar(cache: AladhanCacheDataSource): MediatorRegistrar = object : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.register(getPrayerTimesHandler(cache))
    }
}
