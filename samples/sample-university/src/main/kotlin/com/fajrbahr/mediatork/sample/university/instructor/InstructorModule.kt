package com.fajrbahr.mediatork.sample.university.instructor

import com.fajrbahr.mediatork.MediatorBuilder
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.createedit.createEditInstructorHandler
import com.fajrbahr.mediatork.sample.university.instructor.createedit.createEditInstructorQueryHandler
import com.fajrbahr.mediatork.sample.university.instructor.createedit.createEditInstructorQueryValidator
import com.fajrbahr.mediatork.sample.university.instructor.createedit.createEditInstructorValidator
import com.fajrbahr.mediatork.sample.university.instructor.delete.deleteInstructorHandler
import com.fajrbahr.mediatork.sample.university.instructor.delete.deleteInstructorQueryHandler
import com.fajrbahr.mediatork.sample.university.instructor.delete.deleteInstructorQueryValidator
import com.fajrbahr.mediatork.sample.university.instructor.detail.getInstructorHandler
import com.fajrbahr.mediatork.sample.university.instructor.list.getInstructorsHandler
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun MediatorBuilder.instructorModule(
    store: InstructorStore,
    departmentStore: DepartmentStore,
    courseStore: CourseStore,
    studentStore: StudentStore,
) {
    handle(getInstructorsHandler(store, departmentStore, courseStore, studentStore))
    handle(getInstructorHandler(store, courseStore))

    handle(createEditInstructorQueryHandler(store))
    validate(createEditInstructorQueryValidator)

    handle(createEditInstructorHandler(store))
    validate(createEditInstructorValidator)

    handle(deleteInstructorQueryHandler(store))
    validate(deleteInstructorQueryValidator)

    handle(deleteInstructorHandler(store, departmentStore))
}
