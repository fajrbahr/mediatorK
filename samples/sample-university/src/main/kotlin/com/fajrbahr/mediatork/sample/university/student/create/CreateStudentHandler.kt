package com.fajrbahr.mediatork.sample.university.student.create

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.student.StudentStore
import com.fajrbahr.mediatork.sample.university.student.model.Student

fun createStudentHandler(
    store: StudentStore,
): Handler<CreateStudentCommand, Int> = { request ->
    val student = Student(
        id = store.nextId(),
        lastName = request.lastName,
        firstMidName = request.firstMidName,
        enrollmentDate = request.enrollmentDate,
    )
    store.save(student)
    student.id
}
