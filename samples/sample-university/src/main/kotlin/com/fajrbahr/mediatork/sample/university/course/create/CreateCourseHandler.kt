package com.fajrbahr.mediatork.sample.university.course.create

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.course.model.Course

fun createCourseHandler(
    store: CourseStore,
): Handler<CreateCourseCommand, Int> = { request ->
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
