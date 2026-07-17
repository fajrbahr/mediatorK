package com.fajrbahr.mediatork.sample.university.course.edit

import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.validator.rules

val editCourseValidator: Validator<EditCourseCommand> = { request ->
    rules {
        check(request.title.length in 3..50) { "Title must be between 3 and 50 characters" }
        check(request.credits in 0..5) { "Credits must be between 0 and 5" }
    }
}
