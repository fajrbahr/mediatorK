package com.fajrbahr.mediatork.sample.university.department

import com.fajrbahr.mediatork.MediatorBuilder
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.create.createDepartmentHandler
import com.fajrbahr.mediatork.sample.university.department.create.createDepartmentValidator
import com.fajrbahr.mediatork.sample.university.department.delete.deleteDepartmentHandler
import com.fajrbahr.mediatork.sample.university.department.delete.deleteDepartmentQueryHandler
import com.fajrbahr.mediatork.sample.university.department.delete.deleteDepartmentQueryValidator
import com.fajrbahr.mediatork.sample.university.department.detail.getDepartmentHandler
import com.fajrbahr.mediatork.sample.university.department.edit.editDepartmentHandler
import com.fajrbahr.mediatork.sample.university.department.edit.editDepartmentQueryHandler
import com.fajrbahr.mediatork.sample.university.department.edit.editDepartmentQueryValidator
import com.fajrbahr.mediatork.sample.university.department.edit.editDepartmentValidator
import com.fajrbahr.mediatork.sample.university.department.list.getDepartmentsHandler
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

fun MediatorBuilder.departmentModule(
    store: DepartmentStore,
    instructorStore: InstructorStore,
    courseStore: CourseStore,
) {
    handle(getDepartmentsHandler(store, instructorStore))
    handle(getDepartmentHandler(store, instructorStore, courseStore))

    handle(createDepartmentHandler(store))
    validate(createDepartmentValidator)

    handle(editDepartmentQueryHandler(store))
    validate(editDepartmentQueryValidator)

    handle(editDepartmentHandler(store))
    validate(editDepartmentValidator)

    handle(deleteDepartmentQueryHandler(store, instructorStore))
    validate(deleteDepartmentQueryValidator)

    handle(deleteDepartmentHandler(store))
}
