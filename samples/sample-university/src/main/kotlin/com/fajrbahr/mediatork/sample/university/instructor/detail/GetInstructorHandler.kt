package com.fajrbahr.mediatork.sample.university.instructor.detail

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

fun getInstructorHandler(
    store: InstructorStore,
    courseStore: CourseStore,
): Handler<GetInstructorQuery, InstructorDetailModel?> = handler@{ request ->
    val instructor = store.findById(request.id) ?: return@handler null
    InstructorDetailModel(
        id = instructor.id,
        lastName = instructor.lastName,
        firstMidName = instructor.firstMidName,
        hireDate = instructor.hireDate,
        officeLocation = instructor.officeLocation,
        courses = instructor.courseIds.mapNotNull { courseId ->
            val course = courseStore.findById(courseId) ?: return@mapNotNull null
            InstructorDetailModel.CourseModel(id = course.id, title = course.title)
        },
    )
}
