package com.fajrbahr.mediatork.sample.university.instructor.createedit

import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.validator.rules

val createEditInstructorQueryValidator: Validator<CreateEditInstructorQuery> = { request ->
    rules {
        check(request.id != null) { "Id is required" }
    }
}
