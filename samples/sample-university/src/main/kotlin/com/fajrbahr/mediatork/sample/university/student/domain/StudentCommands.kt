package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.model.Enrollment
import com.fajrbahr.mediatork.sample.university.student.model.Student
import com.fajrbahr.mediatork.validator.rules

// ── Queries ──────────────────────────────────────────────────────────────────

data object GetStudentsQuery : Request<List<Student>>

class GetStudentsHandler(
    private val store: StudentStore,
) : RequestHandler<GetStudentsQuery, List<Student>> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetStudentsQuery,
    ): List<Student> = store.findAll()
}

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

// ── Create ───────────────────────────────────────────────────────────────────

data class CreateStudentCommand(
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
) : Request<Int> {
    override fun validate() = rules<String> {
        check(lastName.length in 1..50) { "Last name must be between 1 and 50 characters" }
        check(firstMidName.length in 1..50) { "First name must be between 1 and 50 characters" }
        check(enrollmentDate.isNotBlank()) { "Enrollment date is required" }
    }
}

class CreateStudentHandler(
    private val store: StudentStore,
) : RequestHandler<CreateStudentCommand, Int> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateStudentCommand,
    ): Int {
        val student = Student(
            id = store.nextId(),
            lastName = request.lastName,
            firstMidName = request.firstMidName,
            enrollmentDate = request.enrollmentDate,
        )
        store.save(student)
        return student.id
    }
}

// ── Edit ─────────────────────────────────────────────────────────────────────

data class EditStudentCommand(
    val id: Int = 0,
    val lastName: String = "",
    val firstMidName: String = "",
    val enrollmentDate: String = "",
) : Request<Unit> {
    override fun validate() = rules<String> {
        check(lastName.length in 1..50) { "Last name must be between 1 and 50 characters" }
        check(firstMidName.length in 1..50) { "First name must be between 1 and 50 characters" }
        check(enrollmentDate.isNotBlank()) { "Enrollment date is required" }
    }
}

class EditStudentHandler(
    private val store: StudentStore,
) : RequestHandler<EditStudentCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: EditStudentCommand,
    ) {
        val existing = store.findById(request.id) ?: return
        store.save(
            existing.copy(
                lastName = request.lastName,
                firstMidName = request.firstMidName,
                enrollmentDate = request.enrollmentDate,
            )
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
