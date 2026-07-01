package com.fajrbahr.mediatork.sample.university.instructor.domain

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor
import com.fajrbahr.mediatork.validator.rules

// ── Queries ──────────────────────────────────────────────────────────────────

data object GetInstructorsQuery : Request<List<Instructor>>

class GetInstructorsHandler(
    private val store: InstructorStore,
) : RequestHandler<GetInstructorsQuery, List<Instructor>> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetInstructorsQuery,
    ): List<Instructor> = store.findAll()
}

data class GetInstructorQuery(val id: Int) : Request<Instructor?>

class GetInstructorHandler(
    private val store: InstructorStore,
) : RequestHandler<GetInstructorQuery, Instructor?> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: GetInstructorQuery,
    ): Instructor? = store.findById(request.id)
}

// ── CreateEdit (combined, matching Contoso) ──────────────────────────────────

data class CreateEditInstructorCommand(
    val id: Int? = null,
    val lastName: String = "",
    val firstMidName: String = "",
    val hireDate: String = "",
    val officeLocation: String? = null,
    val selectedCourseIds: List<Int> = emptyList(),
) : Request<Int> {
    override fun validate() = rules<String> {
        check(lastName.length in 1..50) { "Last name must be between 1 and 50 characters" }
        check(firstMidName.length in 1..50) { "First name must be between 1 and 50 characters" }
        check(hireDate.isNotBlank()) { "Hire date is required" }
    }
}

class CreateEditInstructorHandler(
    private val store: InstructorStore,
    private val departmentStore: DepartmentStore,
) : RequestHandler<CreateEditInstructorCommand, Int> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: CreateEditInstructorCommand,
    ): Int {
        return if (request.id == null) {
            val instructor = Instructor(
                id = store.nextId(),
                lastName = request.lastName,
                firstMidName = request.firstMidName,
                hireDate = request.hireDate,
                officeLocation = request.officeLocation,
                courseIds = request.selectedCourseIds,
            )
            store.save(instructor)
            instructor.id
        } else {
            val existing = store.findById(request.id) ?: return request.id
            store.save(
                existing.copy(
                    lastName = request.lastName,
                    firstMidName = request.firstMidName,
                    hireDate = request.hireDate,
                    officeLocation = request.officeLocation,
                    courseIds = request.selectedCourseIds,
                )
            )
            request.id
        }
    }
}

// ── Delete ───────────────────────────────────────────────────────────────────

data class DeleteInstructorCommand(val id: Int) : Request<Unit>

class DeleteInstructorHandler(
    private val store: InstructorStore,
    private val departmentStore: DepartmentStore,
) : RequestHandler<DeleteInstructorCommand, Unit> {
    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: DeleteInstructorCommand,
    ) {
        store.delete(request.id)
        for (dept in departmentStore.findAll()) {
            if (dept.administratorId == request.id) {
                departmentStore.save(dept.copy(administratorId = null))
            }
        }
    }
}
