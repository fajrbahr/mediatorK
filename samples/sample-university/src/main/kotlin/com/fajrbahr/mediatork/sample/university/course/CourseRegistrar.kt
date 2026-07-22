package com.fajrbahr.mediatork.sample.university.course

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseHandler
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseValidator
import com.fajrbahr.mediatork.sample.university.course.delete.DeleteCourseHandler
import com.fajrbahr.mediatork.sample.university.course.delete.DeleteCourseQueryHandler
import com.fajrbahr.mediatork.sample.university.course.delete.DeleteCourseQueryValidator
import com.fajrbahr.mediatork.sample.university.course.detail.GetCourseHandler
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseHandler
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseQueryHandler
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseQueryValidator
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseValidator
import com.fajrbahr.mediatork.sample.university.course.list.GetCoursesHandler
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.student.StudentStore

class CourseRegistrar(
    private val store: CourseStore,
    private val departmentStore: DepartmentStore,
    private val studentStore: StudentStore,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.registerLazy { GetCoursesHandler(store, departmentStore) }
        registry.registerLazy { GetCourseHandler(store, departmentStore, studentStore) }
        registry.registerLazy { CreateCourseHandler(store) }
        registry.registerValidator(CreateCourseValidator())
        registry.registerLazy { EditCourseQueryHandler(store) }
        registry.registerValidator(EditCourseQueryValidator())
        registry.registerLazy { EditCourseHandler(store) }
        registry.registerValidator(EditCourseValidator())
        registry.registerLazy { DeleteCourseQueryHandler(store, departmentStore) }
        registry.registerValidator(DeleteCourseQueryValidator())
        registry.registerLazy { DeleteCourseHandler(store) }
    }
}
