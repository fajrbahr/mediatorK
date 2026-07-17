package com.fajrbahr.mediatork.sample.university.student.delete

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun deleteStudentQueryHandler(
    store: StudentStore,
): Handler<DeleteStudentQuery, DeleteStudentCommand?> = handler@{ request ->
    val student = store.findById(request.id!!) ?: return@handler null
    DeleteStudentCommand(
        id = student.id,
        lastName = student.lastName,
        firstMidName = student.firstMidName,
        enrollmentDate = student.enrollmentDate,
    )
}
