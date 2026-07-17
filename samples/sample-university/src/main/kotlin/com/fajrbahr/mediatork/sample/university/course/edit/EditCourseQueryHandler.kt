package com.fajrbahr.mediatork.sample.university.course.edit

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.course.CourseStore

fun editCourseQueryHandler(
    store: CourseStore,
): Handler<EditCourseQuery, EditCourseCommand?> = handler@{ request ->
    val course = store.findById(request.id!!) ?: return@handler null
    EditCourseCommand(
        id = course.id,
        number = course.number,
        title = course.title,
        credits = course.credits,
        departmentId = course.departmentId,
    )
}
