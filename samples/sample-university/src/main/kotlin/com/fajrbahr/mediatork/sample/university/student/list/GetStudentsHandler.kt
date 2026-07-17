package com.fajrbahr.mediatork.sample.university.student.list

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun getStudentsHandler(
    store: StudentStore,
): Handler<GetStudentsQuery, List<StudentListModel>> = { request ->
    store.findAll().map { student ->
        StudentListModel(
            id = student.id,
            lastName = student.lastName,
            firstMidName = student.firstMidName,
            enrollmentDate = student.enrollmentDate,
            enrollmentsCount = store.findEnrollmentsByStudentId(student.id).size,
        )
    }
}
