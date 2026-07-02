package com.fajrbahr.mediatork.sample.university.instructor.domain

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor
import com.fajrbahr.mediatork.validator.rules

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
    private val departmentStore: com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore,
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
