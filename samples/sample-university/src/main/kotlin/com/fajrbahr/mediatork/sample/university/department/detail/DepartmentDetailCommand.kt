package com.fajrbahr.mediatork.sample.university.department.detail

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

// ── Query ───────────────────────────────────────────────────────────────────

data class GetDepartmentQuery(val id: Int) : Request<DepartmentDetailModel?>

data class DepartmentDetailModel(
    val id: Int,
    val name: String,
    val budget: Double,
    val startDate: String,
    val administratorFullName: String,
)

class GetDepartmentHandler(
    private val store: DepartmentStore,
    private val instructorStore: InstructorStore,
) : RequestHandler<GetDepartmentQuery, DepartmentDetailModel?> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetDepartmentQuery,
    ): DepartmentDetailModel? {
        val dept = store.findById(request.id) ?: return null
        val administrator = dept.administratorId?.let { instructorStore.findById(it) }
        return DepartmentDetailModel(
            id = dept.id,
            name = dept.name,
            budget = dept.budget,
            startDate = dept.startDate,
            administratorFullName = administrator?.fullName ?: "",
        )
    }
}

