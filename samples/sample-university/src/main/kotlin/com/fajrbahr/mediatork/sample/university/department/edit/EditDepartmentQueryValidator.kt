package com.fajrbahr.mediatork.sample.university.department.edit

import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.validator.rules

val editDepartmentQueryValidator: Validator<EditDepartmentQuery> = { request ->
    rules {
        check(request.id != null) { "Id is required" }
    }
}
