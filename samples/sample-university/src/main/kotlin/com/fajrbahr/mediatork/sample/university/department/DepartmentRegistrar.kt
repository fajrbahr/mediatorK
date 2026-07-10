package com.fajrbahr.mediatork.sample.university.department

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.department.create.CreateDepartmentHandler
import com.fajrbahr.mediatork.sample.university.department.create.CreateDepartmentValidator
import com.fajrbahr.mediatork.sample.university.department.delete.DeleteDepartmentHandler
import com.fajrbahr.mediatork.sample.university.department.delete.DeleteDepartmentQueryHandler
import com.fajrbahr.mediatork.sample.university.department.delete.DeleteDepartmentQueryValidator
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
        registry.registerLazy { GetDepartmentsHandler(store, instructorStore) }
        registry.registerLazy { GetDepartmentHandler(store, instructorStore) }
        registry.registerLazy { CreateDepartmentHandler(store) }
        registry.registerValidator(CreateDepartmentValidator())
        registry.registerLazy { EditDepartmentQueryHandler(store) }
        registry.registerValidator(EditDepartmentQueryValidator())
        registry.registerLazy { EditDepartmentHandler(store) }
        registry.registerValidator(EditDepartmentValidator())
        registry.registerLazy { DeleteDepartmentQueryHandler(store, instructorStore) }
        registry.registerValidator(DeleteDepartmentQueryValidator())
        registry.registerLazy { DeleteDepartmentHandler(store) }
    }
}
