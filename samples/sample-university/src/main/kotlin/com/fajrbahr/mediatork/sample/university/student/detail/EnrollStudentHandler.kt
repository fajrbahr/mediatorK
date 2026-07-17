package com.fajrbahr.mediatork.sample.university.student.detail

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.model.Enrollment
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun enrollStudentHandler(
    store: StudentStore,
): Handler<EnrollStudentCommand, Int> = { request ->
    val enrollment = Enrollment(
        id = store.nextEnrollmentId(),
        courseId = request.courseId,
        studentId = request.studentId,
        grade = request.grade,
    )
    store.saveEnrollment(enrollment)
    enrollment.id
}
