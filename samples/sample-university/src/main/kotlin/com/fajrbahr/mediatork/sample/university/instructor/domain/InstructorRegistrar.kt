package com.fajrbahr.mediatork.sample.university.instructor.domain

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore

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
