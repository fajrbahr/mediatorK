package com.fajrbahr.mediatork.sample.university.student.domain

import com.fajrbahr.mediatork.api.Request
import com.fajrbahr.mediatork.feature.Feature
import com.fajrbahr.mediatork.feature.feature
import kotlin.time.Duration.Companion.seconds

data class DeleteStudentCommand(val id: Int) : Request<Unit>

fun deleteStudent(store: StudentStore): Feature<DeleteStudentCommand, Unit> =
    feature {
        handle { request ->
            store.delete(request.id)
        }
            .timeout(2.seconds)
            .measure()
    }
