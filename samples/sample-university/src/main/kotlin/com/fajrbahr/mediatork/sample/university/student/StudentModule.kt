package com.fajrbahr.mediatork.sample.university.student

import com.fajrbahr.mediatork.MediatorBuilder
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.student.create.createStudentHandler
import com.fajrbahr.mediatork.sample.university.student.create.createStudentValidator
import com.fajrbahr.mediatork.sample.university.student.delete.deleteStudentHandler
import com.fajrbahr.mediatork.sample.university.student.delete.deleteStudentQueryHandler
import com.fajrbahr.mediatork.sample.university.student.delete.deleteStudentQueryValidator
import com.fajrbahr.mediatork.sample.university.student.detail.enrollStudentHandler
import com.fajrbahr.mediatork.sample.university.student.detail.getStudentHandler
import com.fajrbahr.mediatork.sample.university.student.edit.editStudentHandler
import com.fajrbahr.mediatork.sample.university.student.edit.editStudentQueryHandler
import com.fajrbahr.mediatork.sample.university.student.edit.editStudentQueryValidator
import com.fajrbahr.mediatork.sample.university.student.edit.editStudentValidator
import com.fajrbahr.mediatork.sample.university.student.list.getStudentsHandler

fun MediatorBuilder.studentModule(
    store: StudentStore,
    courseStore: CourseStore,
) {
    handle(getStudentsHandler(store))
    handle(getStudentHandler(store, courseStore))

    handle(createStudentHandler(store))
    validate(createStudentValidator)

    handle(editStudentQueryHandler(store))
    validate(editStudentQueryValidator)

    handle(editStudentHandler(store))
    validate(editStudentValidator)

    handle(deleteStudentQueryHandler(store))
    validate(deleteStudentQueryValidator)

    handle(deleteStudentHandler(store))

    handle(enrollStudentHandler(store))
}
