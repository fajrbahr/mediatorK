package com.fajrbahr.mediatork.sample.university.instructor.delete

import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.validator.rules

val deleteInstructorQueryValidator: Validator<DeleteInstructorQuery> = { request ->
    rules {
        check(request.id != null) { "Id is required" }
    }
}
