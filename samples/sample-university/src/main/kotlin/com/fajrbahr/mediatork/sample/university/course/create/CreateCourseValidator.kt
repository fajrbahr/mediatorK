package com.fajrbahr.mediatork.sample.university.course.create

import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.validator.rules

val createCourseValidator: Validator<CreateCourseCommand> = { request ->
    rules {
        check(request.number > 0) { "Number must be greater than 0" }
        check(request.title.length in 3..50) { "Title must be between 3 and 50 characters" }
        check(request.credits in 0..5) { "Credits must be between 0 and 5" }
    }
}
