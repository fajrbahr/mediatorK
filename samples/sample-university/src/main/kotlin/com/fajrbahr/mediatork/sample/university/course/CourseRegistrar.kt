package com.fajrbahr.mediatork.sample.university.course

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseHandler
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseValidator
import com.fajrbahr.mediatork.sample.university.course.detail.DeleteCourseHandler
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseHandler
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseHandler
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseQueryHandler
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseQueryValidator
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseValidator
import com.fajrbahr.mediatork.sample.university.course.list.GetCoursesHandler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore

class CourseRegistrar(
    private val store: CourseStore,
    private val departmentStore: DepartmentStore,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetCoursesHandler(store, departmentStore)
        registry register GetCourseHandler(store, departmentStore)
        registry register CreateCourseHandler(store)
        registry.registerValidator(CreateCourseValidator())
        registry register EditCourseQueryHandler(store)
        registry.registerValidator(EditCourseQueryValidator())
        registry register EditCourseHandler(store)
        registry.registerValidator(EditCourseValidator())
        registry register DeleteCourseHandler(store)
    }
}
