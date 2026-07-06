package com.fajrbahr.mediatork.sample.university.department.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import kotlin.time.Duration.Companion.seconds

data class DeleteDepartmentCommand(val id: Int) : Request<Unit>

fun deleteDepartment(store: DepartmentStore): Feature<DeleteDepartmentCommand, Unit> =
    feature {
        handle { request ->
            store.delete(request.id)
        }
            .timeout(2.seconds)
            .measure()
    }
