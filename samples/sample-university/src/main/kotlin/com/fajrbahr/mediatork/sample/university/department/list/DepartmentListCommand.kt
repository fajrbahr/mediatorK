package com.fajrbahr.mediatork.sample.university.department.list

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.department.model.Department

data object GetDepartmentsQuery : Request<List<Department>>

class GetDepartmentsHandler(
    private val store: DepartmentStore,
) : RequestHandler<GetDepartmentsQuery, List<Department>> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetDepartmentsQuery,
    ): List<Department> = store.findAll()
}
