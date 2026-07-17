package com.fajrbahr.mediatork.sample.university.student.edit

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun editStudentQueryHandler(
    store: StudentStore,
): Handler<EditStudentQuery, EditStudentCommand?> = handler@{ request ->
    val student = store.findById(request.id!!) ?: return@handler null
    EditStudentCommand(
        id = student.id,
        lastName = student.lastName,
        firstMidName = student.firstMidName,
        enrollmentDate = student.enrollmentDate,
    )
}
