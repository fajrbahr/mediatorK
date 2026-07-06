package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.validator
import com.fajrbahr.mediatork.validator.rules
import kotlin.time.Duration.Companion.seconds

data class EditDepartmentCommand(
    val id: Int = 0,
    val name: String = "",
    val budget: Double = 0.0,
    val startDate: String = "",
    val administratorId: Int? = null,
) : Request<Unit>

val editDepartmentValidator = validator<EditDepartmentCommand> { request ->
    rules<String> {
        check(request.name.length in 3..50) { "Name must be between 3 and 50 characters" }
        check(request.budget >= 0) { "Budget must be non-negative" }
        check(request.startDate.isNotBlank()) { "Start date is required" }
    }
}

fun editDepartment(store: DepartmentStore): Feature<EditDepartmentCommand, Unit> =
    feature {
        handle { request ->
            val existing = store.findById(request.id) ?: return@handle
            store.save(
                existing.copy(
                    name = request.name,
                    budget = request.budget,
                    startDate = request.startDate,
                    administratorId = request.administratorId,
                )
            )
        }
            .timeout(3.seconds)
            .measure()

        validate(editDepartmentValidator)
    }
