package com.fajrbahr.mediatork.sample.university.instructor.createedit

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

fun createEditInstructorQueryHandler(
    store: InstructorStore,
): Handler<CreateEditInstructorQuery, CreateEditInstructorCommand> = handler@{ request ->
    if (request.id == null) {
        return@handler CreateEditInstructorCommand()
    }
    val instructor = store.findById(request.id) ?: return@handler CreateEditInstructorCommand()
    CreateEditInstructorCommand(
        id = instructor.id,
        lastName = instructor.lastName,
        firstMidName = instructor.firstMidName,
        hireDate = instructor.hireDate,
        officeLocation = instructor.officeLocation,
        selectedCourseIds = instructor.courseIds,
    )
}
