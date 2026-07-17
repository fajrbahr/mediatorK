package com.fajrbahr.mediatork.sample.university.course.edit

import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.validator.rules

val editCourseQueryValidator: Validator<EditCourseQuery> = { request ->
    rules {
        check(request.id != null) { "Id is required" }
    }
}
