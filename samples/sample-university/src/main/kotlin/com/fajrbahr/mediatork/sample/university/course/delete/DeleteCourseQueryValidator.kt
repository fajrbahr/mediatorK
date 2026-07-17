package com.fajrbahr.mediatork.sample.university.course.delete

import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.validator.rules

val deleteCourseQueryValidator: Validator<DeleteCourseQuery> = { request ->
    rules {
        check(request.id != null) { "Id is required" }
    }
}
