package com.fajrbahr.mediatork.sample.university.instructor.list

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore
import com.fajrbahr.mediatork.sample.university.model.Grade
import com.fajrbahr.mediatork.sample.university.student.StudentStore

data class GetInstructorsQuery(
    val selectedInstructorId: Int? = null,
    val selectedCourseId: Int? = null,
) : Request<InstructorIndexModel>

data class InstructorIndexModel(
    val instructors: List<InstructorRow>,
    val courses: List<CourseRow> = emptyList(),
    val enrollments: List<EnrollmentRow> = emptyList(),
    val selectedInstructorId: Int? = null,
    val selectedCourseId: Int? = null,
) {
    data class InstructorRow(
        val id: Int,
        val lastName: String,
        val firstMidName: String,
        val hireDate: String,
        val officeLocation: String?,
    )

    data class CourseRow(
        val id: Int,
        val number: Int,
        val title: String,
        val departmentName: String,
    )

    data class EnrollmentRow(
        val studentFullName: String,
        val grade: Grade?,
    )
}

class GetInstructorsHandler(
    private val store: InstructorStore,
    private val courseStore: CourseStore,
    private val departmentStore: DepartmentStore,
    private val studentStore: StudentStore,
) : RequestHandler<GetInstructorsQuery, InstructorIndexModel> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetInstructorsQuery,
    ): InstructorIndexModel {
        val instructors = store.findAll().map { instructor ->
            InstructorIndexModel.InstructorRow(
                id = instructor.id,
                lastName = instructor.lastName,
                firstMidName = instructor.firstMidName,
                hireDate = instructor.hireDate,
                officeLocation = instructor.officeLocation,
            )
        }

        val courses = if (request.selectedInstructorId != null) {
            val instructor = store.findById(request.selectedInstructorId)
            instructor?.courseIds?.mapNotNull { courseId ->
                val course = courseStore.findById(courseId) ?: return@mapNotNull null
                val dept = departmentStore.findById(course.departmentId)
                InstructorIndexModel.CourseRow(
                    id = course.id,
                    number = course.number,
                    title = course.title,
                    departmentName = dept?.name ?: "",
                )
            } ?: emptyList()
        } else {
            emptyList()
        }

        val enrollments = if (request.selectedCourseId != null) {
            studentStore.findEnrollmentsByCourseId(request.selectedCourseId).map { enrollment ->
                val student = studentStore.findById(enrollment.studentId)
                InstructorIndexModel.EnrollmentRow(
                    studentFullName = student?.fullName ?: "Unknown",
                    grade = enrollment.grade,
                )
            }
        } else {
            emptyList()
        }

        return InstructorIndexModel(
            instructors = instructors,
            courses = courses,
            enrollments = enrollments,
            selectedInstructorId = request.selectedInstructorId,
            selectedCourseId = request.selectedCourseId,
        )
    }
}
