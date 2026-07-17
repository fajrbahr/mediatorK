package com.fajrbahr.mediatork.sample.university.department.detail

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

fun getDepartmentHandler(
    store: DepartmentStore,
    instructorStore: InstructorStore,
    courseStore: CourseStore,
): Handler<GetDepartmentQuery, DepartmentDetailModel?> = handler@{ request ->
    val dept = store.findById(request.id) ?: return@handler null
    val administrator = dept.administratorId?.let { instructorStore.findById(it) }
    val courses = courseStore.findAll().filter { it.departmentId == dept.id }
    DepartmentDetailModel(
        id = dept.id,
        name = dept.name,
        budget = dept.budget,
        startDate = dept.startDate,
        administratorFullName = administrator?.fullName ?: "",
        courses = courses.map { course ->
            DepartmentDetailModel.CourseModel(
                id = course.id,
                title = course.title,
                credits = course.credits,
            )
        },
    )
}
