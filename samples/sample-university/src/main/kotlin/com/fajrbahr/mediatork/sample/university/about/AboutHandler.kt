package com.fajrbahr.mediatork.sample.university.about

import com.fajrbahr.mediatork.Handler
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun aboutHandler(
    store: StudentStore,
): Handler<AboutQuery, AboutResult> = { request ->
    val students = store.findAll()
    val groups = students
        .groupBy { it.enrollmentDate }
        .map { (date, list) ->
            AboutResult.EnrollmentDateGroup(
                enrollmentDate = date,
                studentCount = list.size,
            )
        }
        .sortedBy { it.enrollmentDate }
    AboutResult(items = groups)
}
