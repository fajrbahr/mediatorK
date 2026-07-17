package com.fajrbahr.mediatork.sample.university.course.delete

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.course.CourseStore

fun deleteCourseHandler(
    store: CourseStore,
): Handler<DeleteCourseCommand, Unit> = { request ->
    store.delete(request.id)
}
