package com.fajrbahr.mediatork.sample.university.instructor.list

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

data object GetInstructorsQuery : Request<List<InstructorListModel>>

data class InstructorListModel(
    val id: Int,
    val lastName: String,
    val firstMidName: String,
    val hireDate: String,
    val officeLocation: String?,
)

class GetInstructorsHandler(
    private val store: InstructorStore,
) : RequestHandler<GetInstructorsQuery, List<InstructorListModel>> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetInstructorsQuery,
    ): List<InstructorListModel> = store.findAll().map { instructor ->
        InstructorListModel(
            id = instructor.id,
            lastName = instructor.lastName,
            firstMidName = instructor.firstMidName,
            hireDate = instructor.hireDate,
            officeLocation = instructor.officeLocation,
        )
    }
}
