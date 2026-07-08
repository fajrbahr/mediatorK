package com.fajrbahr.mediatork.sample.university.department.list

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

data object GetDepartmentsQuery : Request<List<DepartmentListModel>>

data class DepartmentListModel(
    val id: Int,
    val name: String,
    val budget: Double,
    val startDate: String,
    val administratorFullName: String,
)

class GetDepartmentsHandler(
    private val store: DepartmentStore,
    private val instructorStore: InstructorStore,
) : RequestHandler<GetDepartmentsQuery, List<DepartmentListModel>> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetDepartmentsQuery,
    ): List<DepartmentListModel> = store.findAll().map { dept ->
        val administrator = dept.administratorId?.let { instructorStore.findById(it) }
        DepartmentListModel(
            id = dept.id,
            name = dept.name,
            budget = dept.budget,
            startDate = dept.startDate,
            administratorFullName = administrator?.fullName ?: "",
        )
    }
}
