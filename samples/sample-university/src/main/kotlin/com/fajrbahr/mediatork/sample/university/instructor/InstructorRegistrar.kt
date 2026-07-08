package com.fajrbahr.mediatork.sample.university.instructor

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorHandler
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorValidator
import com.fajrbahr.mediatork.sample.university.instructor.detail.DeleteInstructorHandler
import com.fajrbahr.mediatork.sample.university.instructor.detail.GetInstructorHandler
import com.fajrbahr.mediatork.sample.university.instructor.list.GetInstructorsHandler

class InstructorRegistrar(
    private val store: InstructorStore,
    private val departmentStore: DepartmentStore,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetInstructorsHandler(store)
        registry register GetInstructorHandler(store)
        registry register CreateEditInstructorHandler(store, departmentStore)
        registry.registerValidator(CreateEditInstructorValidator())
        registry register DeleteInstructorHandler(store, departmentStore)
    }
}
