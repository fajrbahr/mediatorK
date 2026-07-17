package com.fajrbahr.mediatork.sample.university.student.edit

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun editStudentHandler(
    store: StudentStore,
): Handler<EditStudentCommand, Unit> = handler@{ request ->
    val existing = store.findById(request.id) ?: return@handler
    store.save(
        existing.copy(
            lastName = request.lastName,
            firstMidName = request.firstMidName,
            enrollmentDate = request.enrollmentDate,
        )
    )
}
