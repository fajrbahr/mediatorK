package com.fajrbahr.mediatork.sample.university.department.delete

import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.validator.rules

val deleteDepartmentQueryValidator: Validator<DeleteDepartmentQuery> = { request ->
    rules {
        check(request.id != null) { "Id is required" }
    }
}
