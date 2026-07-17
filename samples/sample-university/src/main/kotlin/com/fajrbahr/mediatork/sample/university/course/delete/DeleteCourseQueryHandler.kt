package com.fajrbahr.mediatork.sample.university.course.delete

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore

fun deleteCourseQueryHandler(
    store: CourseStore,
    departmentStore: DepartmentStore,
): Handler<DeleteCourseQuery, DeleteCourseCommand?> = handler@{ request ->
    val course = store.findById(request.id!!) ?: return@handler null
    val department = departmentStore.findById(course.departmentId)
    DeleteCourseCommand(
        id = course.id,
        title = course.title,
        credits = course.credits,
        departmentName = department?.name ?: "",
    )
}
