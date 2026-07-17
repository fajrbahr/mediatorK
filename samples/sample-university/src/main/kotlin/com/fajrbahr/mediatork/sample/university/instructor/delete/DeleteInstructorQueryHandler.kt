package com.fajrbahr.mediatork.sample.university.instructor.delete

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

fun deleteInstructorQueryHandler(
    store: InstructorStore,
): Handler<DeleteInstructorQuery, DeleteInstructorCommand?> = handler@{ request ->
    val instructor = store.findById(request.id!!) ?: return@handler null
    DeleteInstructorCommand(
        id = instructor.id,
        lastName = instructor.lastName,
        firstMidName = instructor.firstMidName,
        hireDate = instructor.hireDate,
        officeLocation = instructor.officeLocation ?: "",
    )
}
