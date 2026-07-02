package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.department.model.Department
import com.fajrbahr.mediatork.validator.rules

data class CreateDepartmentCommand(
    val name: String = "",
    val budget: Double = 0.0,
    val startDate: String = "",
    val administratorId: Int? = null,
) : Request<Int> {
    override fun validate() = rules<String> {
        check(name.length in 3..50) { "Name must be between 3 and 50 characters" }
        check(budget >= 0) { "Budget must be non-negative" }
        check(startDate.isNotBlank()) { "Start date is required" }
    }
}

class CreateDepartmentHandler(
    private val store: DepartmentStore,
) : RequestHandler<CreateDepartmentCommand, Int> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateDepartmentCommand,
    ): Int {
        val dept = Department(
            id = store.nextId(),
            name = request.name,
            budget = request.budget,
            startDate = request.startDate,
            administratorId = request.administratorId,
        )
        store.save(dept)
        return dept.id
    }
}
