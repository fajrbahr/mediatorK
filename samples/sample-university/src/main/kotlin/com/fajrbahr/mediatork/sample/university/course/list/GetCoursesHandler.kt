package com.fajrbahr.mediatork.sample.university.course.list

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore

fun getCoursesHandler(
    store: CourseStore,
    departmentStore: DepartmentStore,
): Handler<GetCoursesQuery, GetCoursesResult> = { request ->
    val courses = store.findAll().map { course ->
        val department = departmentStore.findById(course.departmentId)
        CourseListModel(
            id = course.id,
            title = course.title,
            credits = course.credits,
            departmentName = department?.name ?: "",
        )
    }
    GetCoursesResult(courses = courses)
}
