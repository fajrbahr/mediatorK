package com.fajrbahr.mediatork.sample.university.course.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.validator
import com.fajrbahr.mediatork.sample.university.course.model.Course
import com.fajrbahr.mediatork.validator.rules

data class GetCourseQuery(val id: Int) : Request<Course?>

fun getCourse(store: CourseStore): Feature<GetCourseQuery, Course?> =
    feature {
        handle { request -> store.findById(request.id) }
    }

data class EditCourseCommand(
    val id: Int = 0,
    val title: String = "",
    val credits: Int = 0,
    val departmentId: Int = 0,
) : Request<Unit>

val editCourseValidator = validator<EditCourseCommand> { request ->
    rules<String> {
        check(request.title.length in 3..50) { "Title must be between 3 and 50 characters" }
        check(request.credits in 0..5) { "Credits must be between 0 and 5" }
    }
}

fun editCourse(store: CourseStore): Feature<EditCourseCommand, Unit> =
    feature {
        validate(editCourseValidator)
        handle { request ->
            val existing = store.findById(request.id) ?: return@handle
            store.save(
                existing.copy(
                    title = request.title,
                    credits = request.credits,
                    departmentId = request.departmentId,
                )
            )
        }
    }
