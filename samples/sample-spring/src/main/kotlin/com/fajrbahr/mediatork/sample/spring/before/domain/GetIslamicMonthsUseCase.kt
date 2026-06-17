package com.fajrbahr.mediatork.sample.spring.before.domain

import com.fajrbahr.mediatork.sample.spring.before.data.repository.AladhanRepository
import com.fajrbahr.mediatork.sample.spring.before.model.IslamicMonth
import org.springframework.stereotype.Service

@Service
class GetIslamicMonthsUseCase(private val repository: AladhanRepository) {
    suspend operator fun invoke(): List<IslamicMonth> =
        repository.getIslamicMonths()
}
