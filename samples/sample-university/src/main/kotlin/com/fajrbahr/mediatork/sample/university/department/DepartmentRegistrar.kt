package com.fajrbahr.mediatork.sample.university.department

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.department.create.CreateDepartmentHandler
import com.fajrbahr.mediatork.sample.university.department.create.CreateDepartmentValidator
import com.fajrbahr.mediatork.sample.university.department.detail.DeleteDepartmentHandler
import com.fajrbahr.mediatork.sample.university.department.detail.GetDepartmentHandler
import com.fajrbahr.mediatork.sample.university.department.edit.EditDepartmentHandler
import com.fajrbahr.mediatork.sample.university.department.edit.EditDepartmentQueryHandler
import com.fajrbahr.mediatork.sample.university.department.edit.EditDepartmentQueryValidator
import com.fajrbahr.mediatork.sample.university.department.edit.EditDepartmentValidator
import com.fajrbahr.mediatork.sample.university.department.list.GetDepartmentsHandler
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore

class DepartmentRegistrar(
    private val store: DepartmentStore,
    private val instructorStore: InstructorStore,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register GetDepartmentsHandler(store, instructorStore)
        registry register GetDepartmentHandler(store, instructorStore)
        registry register CreateDepartmentHandler(store)
        registry.registerValidator(CreateDepartmentValidator())
        registry register EditDepartmentQueryHandler(store)
        registry.registerValidator(EditDepartmentQueryValidator())
        registry register EditDepartmentHandler(store)
        registry.registerValidator(EditDepartmentValidator())
        registry register DeleteDepartmentHandler(store)
    }
}
