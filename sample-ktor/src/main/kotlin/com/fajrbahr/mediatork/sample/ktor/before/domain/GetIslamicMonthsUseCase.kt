package com.fajrbahr.mediatork.sample.ktor.before.domain

import com.fajrbahr.mediatork.sample.ktor.before.data.repository.AladhanRepository
import com.fajrbahr.mediatork.sample.ktor.before.model.IslamicMonth

class GetIslamicMonthsUseCase(private val repository: AladhanRepository) {
    suspend operator fun invoke(): List<IslamicMonth> =
        repository.getIslamicMonths()
}
