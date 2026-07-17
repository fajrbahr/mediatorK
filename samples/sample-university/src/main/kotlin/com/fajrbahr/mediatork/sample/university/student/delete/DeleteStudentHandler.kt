package com.fajrbahr.mediatork.sample.university.student.delete

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun deleteStudentHandler(
    store: StudentStore,
): Handler<DeleteStudentCommand, Unit> = { request ->
    store.delete(request.id)
}
