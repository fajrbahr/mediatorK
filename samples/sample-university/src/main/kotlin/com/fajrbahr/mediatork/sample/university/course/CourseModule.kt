package com.fajrbahr.mediatork.sample.university.course

import com.fajrbahr.mediatork.MediatorBuilder
import com.fajrbahr.mediatork.sample.university.course.create.createCourseHandler
import com.fajrbahr.mediatork.sample.university.course.create.createCourseValidator
import com.fajrbahr.mediatork.sample.university.course.delete.deleteCourseHandler
import com.fajrbahr.mediatork.sample.university.course.delete.deleteCourseQueryHandler
import com.fajrbahr.mediatork.sample.university.course.delete.deleteCourseQueryValidator
import com.fajrbahr.mediatork.sample.university.course.detail.getCourseHandler
import com.fajrbahr.mediatork.sample.university.course.edit.editCourseHandler
import com.fajrbahr.mediatork.sample.university.course.edit.editCourseQueryHandler
import com.fajrbahr.mediatork.sample.university.course.edit.editCourseQueryValidator
import com.fajrbahr.mediatork.sample.university.course.edit.editCourseValidator
import com.fajrbahr.mediatork.sample.university.course.list.getCoursesHandler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun MediatorBuilder.courseModule(
    store: CourseStore,
    departmentStore: DepartmentStore,
    studentStore: StudentStore,
) {
    handle(getCoursesHandler(store, departmentStore))
    handle(getCourseHandler(store, departmentStore, studentStore))

    handle(createCourseHandler(store))
    validate(createCourseValidator)

    handle(editCourseQueryHandler(store))
    validate(editCourseQueryValidator)

    handle(editCourseHandler(store))
    validate(editCourseValidator)

    handle(deleteCourseQueryHandler(store, departmentStore))
    validate(deleteCourseQueryValidator)

    handle(deleteCourseHandler(store))
}
