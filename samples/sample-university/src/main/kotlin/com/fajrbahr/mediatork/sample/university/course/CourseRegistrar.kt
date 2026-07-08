package com.fajrbahr.mediatork.sample.university.course

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseHandler
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseValidator
import com.fajrbahr.mediatork.sample.university.course.detail.DeleteCourseHandler
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseHandler
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseHandler
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseValidator
import com.fajrbahr.mediatork.sample.university.course.list.GetCoursesHandler

class CourseRegistrar(private val store: CourseStore) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetCoursesHandler(store)
        registry register GetCourseHandler(store)
        registry register CreateCourseHandler(store)
        registry.registerValidator(CreateCourseValidator())
        registry register EditCourseHandler(store)
        registry.registerValidator(EditCourseValidator())
        registry register DeleteCourseHandler(store)
    }
}
