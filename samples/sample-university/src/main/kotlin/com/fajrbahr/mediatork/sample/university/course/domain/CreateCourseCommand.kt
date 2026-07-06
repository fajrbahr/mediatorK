package com.fajrbahr.mediatork.sample.university.course.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.validator
import com.fajrbahr.mediatork.sample.university.course.model.Course
import com.fajrbahr.mediatork.validator.rules
import kotlin.time.Duration.Companion.seconds

data class CreateCourseCommand(
    val number: Int = 0,
    val title: String = "",
    val credits: Int = 0,
    val departmentId: Int = 0,
) : Request<Int>

val createCourseValidator = validator<CreateCourseCommand> { request ->
    rules<String> {
        check(request.number > 0) { "Number must be greater than 0" }
        check(request.title.length in 3..50) { "Title must be between 3 and 50 characters" }
        check(request.credits in 0..5) { "Credits must be between 0 and 5" }
    }
}

fun createCourse(store: CourseStore): Feature<CreateCourseCommand, Int> =
    feature {
        handle { request ->
            val course = Course(
                id = store.nextId(),
                number = request.number,
                title = request.title,
                credits = request.credits,
                departmentId = request.departmentId,
            )
            store.save(course)
            course.id
        }
            .retry(2)
            .timeout(3.seconds)
            .measure()

        validate(createCourseValidator)
    }
