package com.fajrbahr.mediatork.sample.university.instructor.createedit

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor

fun createEditInstructorHandler(
    store: InstructorStore,
): Handler<CreateEditInstructorCommand, Int> = handler@{ request ->
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
        val existing = store.findById(request.id) ?: return@handler request.id
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
