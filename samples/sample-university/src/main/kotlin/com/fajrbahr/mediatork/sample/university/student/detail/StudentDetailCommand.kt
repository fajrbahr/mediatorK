package com.fajrbahr.mediatork.sample.university.student.detail

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.model.Grade
import com.fajrbahr.mediatork.sample.university.student.StudentStore

// ── Query ───────────────────────────────────────────────────────────────────

data class GetStudentQuery(val id: Int) : Request<StudentDetailModel?>

data class StudentDetailModel(
    val id: Int,
    val lastName: String,
    val firstMidName: String,
    val enrollmentDate: String,
    val enrollments: List<EnrollmentModel> = emptyList(),
) {
    data class EnrollmentModel(
        val courseId: Int,
        val grade: Grade? = null,
    )
}

class GetStudentHandler(
    private val store: StudentStore,
) : RequestHandler<GetStudentQuery, StudentDetailModel?> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetStudentQuery,
    ): StudentDetailModel? {
        val student = store.findById(request.id) ?: return null
        val enrollments = store.findEnrollmentsByStudentId(request.id)
        return StudentDetailModel(
            id = student.id,
            lastName = student.lastName,
            firstMidName = student.firstMidName,
            enrollmentDate = student.enrollmentDate,
            enrollments = enrollments.map { e ->
                StudentDetailModel.EnrollmentModel(
                    courseId = e.courseId,
                    grade = e.grade,
                )
            },
        )
    }
}

// ── Delete ───────────────────────────────────────────────────────────────────

data class DeleteStudentCommand(val id: Int) : Request<Unit>

class DeleteStudentHandler(
    private val store: StudentStore,
) : RequestHandler<DeleteStudentCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteStudentCommand,
    ) {
        store.delete(request.id)
    }
}

// ── Enroll ───────────────────────────────────────────────────────────────────

data class EnrollStudentCommand(
    val studentId: Int,
    val courseId: Int,
    val grade: Grade? = null,
) : Request<Int>

class EnrollStudentHandler(
    private val store: StudentStore,
) : RequestHandler<EnrollStudentCommand, Int> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: EnrollStudentCommand,
    ): Int {
        val enrollment = com.fajrbahr.mediatork.sample.university.model.Enrollment(
            id = store.nextEnrollmentId(),
            courseId = request.courseId,
            studentId = request.studentId,
            grade = request.grade,
        )
        store.saveEnrollment(enrollment)
        return enrollment.id
    }
}
