package com.fajrbahr.mediatork.sample.university.instructor.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import com.fajrbahr.mediatork.sample.university.department.domain.DepartmentStore
import kotlin.time.Duration.Companion.seconds

data class DeleteInstructorCommand(val id: Int) : Request<Unit>

fun deleteInstructor(
    store: InstructorStore,
    departmentStore: DepartmentStore,
): Feature<DeleteInstructorCommand, Unit> =
    feature {
        handle { request ->
            store.delete(request.id)
            for (dept in departmentStore.findAll()) {
                if (dept.administratorId == request.id) {
                    departmentStore.save(dept.copy(administratorId = null))
                }
            }
        }
            .timeout(3.seconds)
            .measure()
    }
