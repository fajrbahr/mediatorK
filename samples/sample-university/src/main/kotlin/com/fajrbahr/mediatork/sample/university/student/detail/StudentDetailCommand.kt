package com.fajrbahr.mediatork.sample.university.student.detail

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.model.Enrollment
import com.fajrbahr.mediatork.sample.university.student.StudentStore
import com.fajrbahr.mediatork.sample.university.student.model.Student

// ── Queries ──────────────────────────────────────────────────────────────────

data class GetStudentQuery(val id: Int) : Request<Student?>

class GetStudentHandler(
    private val store: StudentStore,
) : RequestHandler<GetStudentQuery, Student?> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetStudentQuery,
    ): Student? = store.findById(request.id)
}

data class GetStudentEnrollmentsQuery(val studentId: Int) : Request<List<Enrollment>>

class GetStudentEnrollmentsHandler(
    private val store: StudentStore,
) : RequestHandler<GetStudentEnrollmentsQuery, List<Enrollment>> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetStudentEnrollmentsQuery,
    ): List<Enrollment> = store.findEnrollmentsByStudentId(request.studentId)
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
    val grade: com.fajrbahr.mediatork.sample.university.model.Grade? = null,
) : Request<Int>

class EnrollStudentHandler(
    private val store: StudentStore,
) : RequestHandler<EnrollStudentCommand, Int> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: EnrollStudentCommand,
    ): Int {
        val enrollment = Enrollment(
            id = store.nextEnrollmentId(),
            courseId = request.courseId,
            studentId = request.studentId,
            grade = request.grade,
        )
        store.saveEnrollment(enrollment)
        return enrollment.id
    }
}
