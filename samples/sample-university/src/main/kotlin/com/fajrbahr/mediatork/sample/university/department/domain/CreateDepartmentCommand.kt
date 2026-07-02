package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.validator
import com.fajrbahr.mediatork.sample.university.department.model.Department
import com.fajrbahr.mediatork.validator.rules

data class CreateDepartmentCommand(
    val name: String = "",
    val budget: Double = 0.0,
    val startDate: String = "",
    val administratorId: Int? = null,
) : Request<Int>

val createDepartmentValidator = validator<CreateDepartmentCommand> { request ->
    rules<String> {
        check(request.name.length in 3..50) { "Name must be between 3 and 50 characters" }
        check(request.budget >= 0) { "Budget must be non-negative" }
        check(request.startDate.isNotBlank()) { "Start date is required" }
    }
}

fun createDepartment(store: DepartmentStore): Feature<CreateDepartmentCommand, Int> =
    feature {
        validate(createDepartmentValidator)
        handle { request ->
            val dept = Department(
                id = store.nextId(),
                name = request.name,
                budget = request.budget,
                startDate = request.startDate,
                administratorId = request.administratorId,
            )
            store.save(dept)
            dept.id
        }
    }
