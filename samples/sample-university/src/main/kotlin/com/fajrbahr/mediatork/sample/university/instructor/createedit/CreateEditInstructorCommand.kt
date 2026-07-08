package com.fajrbahr.mediatork.sample.university.instructor.createedit

import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.api.RequestContext
import com.fajrbahr.mediatork.api.RequestHandler
import com.fajrbahr.mediatork.api.RequestValidator
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor
import com.fajrbahr.mediatork.validator.ValidationResult
import com.fajrbahr.mediatork.validator.rules

data class CreateEditInstructorCommand(
    val id: Int? = null,
    val lastName: String = "",
    val firstMidName: String = "",
    val hireDate: String = "",
    val officeLocation: String? = null,
    val selectedCourseIds: List<Int> = emptyList(),
) : Request<Int>

class CreateEditInstructorValidator : RequestValidator<CreateEditInstructorCommand> {
    override fun validate(request: CreateEditInstructorCommand): ValidationResult = rules {
        check(request.lastName.length in 1..50) { "Last name must be between 1 and 50 characters" }
        check(request.firstMidName.length in 1..50) { "First name must be between 1 and 50 characters" }
        check(request.hireDate.isNotBlank()) { "Hire date is required" }
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
