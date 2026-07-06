package com.fajrbahr.mediatork.sample.university.instructor.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.feature.validator
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor
import com.fajrbahr.mediatork.validator.rules
import kotlin.time.Duration.Companion.seconds

data class CreateEditInstructorCommand(
    val id: Int? = null,
    val lastName: String = "",
    val firstMidName: String = "",
    val hireDate: String = "",
    val officeLocation: String? = null,
    val selectedCourseIds: List<Int> = emptyList(),
) : Request<Int>

val createEditInstructorValidator = validator<CreateEditInstructorCommand> { request ->
    rules<String> {
        check(request.lastName.length in 1..50) { "Last name must be between 1 and 50 characters" }
        check(request.firstMidName.length in 1..50) { "First name must be between 1 and 50 characters" }
        check(request.hireDate.isNotBlank()) { "Hire date is required" }
    }
}

fun createEditInstructor(
    store: InstructorStore,
    departmentStore: DepartmentStore,
): Feature<CreateEditInstructorCommand, Int> =
    feature {
        handle { request ->
            if (request.id == null) {
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
                val existing = store.findById(request.id) ?: return@handle request.id
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
            .retry(2)
            .timeout(3.seconds)
            .measure()

        validate(createEditInstructorValidator)
    }
