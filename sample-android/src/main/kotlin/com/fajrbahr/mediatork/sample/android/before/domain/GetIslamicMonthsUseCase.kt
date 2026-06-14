package com.fajrbahr.mediatork.sample.android.before.domain

import com.fajrbahr.mediatork.sample.android.before.data.repository.AladhanRepository
import com.fajrbahr.mediatork.sample.android.before.model.IslamicMonth

class GetIslamicMonthsUseCase(private val repository: AladhanRepository) {
    suspend operator fun invoke(): List<IslamicMonth> = repository.getIslamicMonths()
}
