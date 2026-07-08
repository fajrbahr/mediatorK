package com.fajrbahr.mediatork.sample.university.instructor

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
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

class InstructorRegistrar(
    private val store: InstructorStore,
    private val departmentStore: DepartmentStore,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetInstructorsHandler(store)
        registry register GetInstructorHandler(store)
        registry register CreateEditInstructorQueryHandler(store)
        registry.registerValidator(CreateEditInstructorQueryValidator())
        registry register CreateEditInstructorHandler(store, departmentStore)
        registry.registerValidator(CreateEditInstructorValidator())
        registry register DeleteInstructorQueryHandler(store)
        registry.registerValidator(DeleteInstructorQueryValidator())
        registry register DeleteInstructorHandler(store, departmentStore)
    }
}
