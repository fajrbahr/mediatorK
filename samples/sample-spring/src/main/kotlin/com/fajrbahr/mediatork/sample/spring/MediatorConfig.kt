package com.fajrbahr.mediatork.sample.spring

import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.MediatorRegistrar
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MediatorConfig {

    @Bean
    fun mediator(registrars: List<MediatorRegistrar>): Mediator =
        MediatorFactory.create(registrars = registrars)
}
