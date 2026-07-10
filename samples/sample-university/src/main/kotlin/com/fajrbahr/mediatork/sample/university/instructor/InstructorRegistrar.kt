package com.fajrbahr.mediatork.sample.university.instructor

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorHandler
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorQueryHandler
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorQueryValidator
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorValidator
import com.fajrbahr.mediatork.sample.university.instructor.delete.DeleteInstructorHandler
import com.fajrbahr.mediatork.sample.university.instructor.delete.DeleteInstructorQueryHandler
import com.fajrbahr.mediatork.sample.university.instructor.delete.DeleteInstructorQueryValidator
import com.fajrbahr.mediatork.sample.university.instructor.detail.GetInstructorHandler
import com.fajrbahr.mediatork.sample.university.instructor.list.GetInstructorsHandler
import com.fajrbahr.mediatork.sample.university.student.StudentStore

class InstructorRegistrar(
    private val store: InstructorStore,
    private val departmentStore: DepartmentStore,
    private val courseStore: CourseStore,
    private val studentStore: StudentStore,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.registerLazy { GetInstructorsHandler(store, courseStore, departmentStore, studentStore) }
        registry.registerLazy { GetInstructorHandler(store, courseStore) }
        registry.registerLazy { CreateEditInstructorQueryHandler(store) }
        registry.registerValidator(CreateEditInstructorQueryValidator())
        registry.registerLazy { CreateEditInstructorHandler(store, departmentStore) }
        registry.registerValidator(CreateEditInstructorValidator())
        registry.registerLazy { DeleteInstructorQueryHandler(store) }
        registry.registerValidator(DeleteInstructorQueryValidator())
        registry.registerLazy { DeleteInstructorHandler(store, departmentStore) }
    }
}
