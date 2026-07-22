package com.fajrbahr.mediatork.sample.university.course.detail

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.model.Grade
import com.fajrbahr.mediatork.sample.university.student.StudentStore

// ── Query ───────────────────────────────────────────────────────────────────

data class GetCourseQuery(val id: Int) : Request<CourseDetailModel?>

data class CourseDetailModel(
    val id: Int,
    val number: Int,
    val title: String,
    val credits: Int,
    val departmentName: String,
    val enrollments: List<EnrollmentModel> = emptyList(),
) {
    data class EnrollmentModel(
        val studentFullName: String,
        val grade: Grade?,
    )
}

class GetCourseHandler(
    private val store: CourseStore,
    private val departmentStore: DepartmentStore,
    private val studentStore: StudentStore,
) : RequestHandler<GetCourseQuery, CourseDetailModel?> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetCourseQuery,
    ): CourseDetailModel? {
        val course = store.findById(request.id) ?: return null
        val department = departmentStore.findById(course.departmentId)
        val enrollments = studentStore.findEnrollmentsByCourseId(course.id)
        return CourseDetailModel(
            id = course.id,
            number = course.number,
            title = course.title,
            credits = course.credits,
            departmentName = department?.name ?: "",
            enrollments = enrollments.map { e ->
                val student = studentStore.findById(e.studentId)
                CourseDetailModel.EnrollmentModel(
                    studentFullName = student?.fullName ?: "Unknown",
                    grade = e.grade,
                )
            },
        )
    }
}
