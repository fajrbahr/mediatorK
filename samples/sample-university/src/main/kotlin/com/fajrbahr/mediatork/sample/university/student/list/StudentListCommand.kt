package com.fajrbahr.mediatork.sample.university.student.list

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.student.StudentStore

data object GetStudentsQuery : Request<List<StudentListModel>>

data class StudentListModel(
    val id: Int,
    val lastName: String,
    val firstMidName: String,
    val enrollmentDate: String,
    val enrollmentsCount: Int,
)

class GetStudentsHandler(
    private val store: StudentStore,
) : RequestHandler<GetStudentsQuery, List<StudentListModel>> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetStudentsQuery,
    ): List<StudentListModel> = store.findAll().map { student ->
        StudentListModel(
            id = student.id,
            lastName = student.lastName,
            firstMidName = student.firstMidName,
            enrollmentDate = student.enrollmentDate,
            enrollmentsCount = store.findEnrollmentsByStudentId(student.id).size,
        )
    }
}
