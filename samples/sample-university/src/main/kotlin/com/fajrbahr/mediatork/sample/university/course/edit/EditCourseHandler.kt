package com.fajrbahr.mediatork.sample.university.course.edit

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.course.CourseStore

fun editCourseHandler(
    store: CourseStore,
): Handler<EditCourseCommand, Unit> = handler@{ request ->
    val existing = store.findById(request.id) ?: return@handler
    store.save(
        existing.copy(
            title = request.title,
            credits = request.credits,
            departmentId = request.departmentId,
        )
    )
}
