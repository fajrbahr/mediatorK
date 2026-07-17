package com.fajrbahr.mediatork.sample.university.department.create

import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.validator.rules

val createDepartmentValidator: Validator<CreateDepartmentCommand> = { request ->
    rules {
        check(request.name.length in 3..50) { "Name must be between 3 and 50 characters" }
        check(request.budget >= 0) { "Budget must be non-negative" }
        check(request.startDate.isNotBlank()) { "Start date is required" }
    }
}
