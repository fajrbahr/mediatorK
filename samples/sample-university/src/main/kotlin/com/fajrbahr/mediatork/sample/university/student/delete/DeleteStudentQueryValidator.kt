package com.fajrbahr.mediatork.sample.university.student.delete

import com.fajrbahr.mediatork.Validator
import com.fajrbahr.mediatork.validator.rules

val deleteStudentQueryValidator: Validator<DeleteStudentQuery> = { request ->
    rules {
        check(request.id != null) { "Id is required" }
    }
}
