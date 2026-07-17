package com.fajrbahr.mediatork.sample.university.about

import com.fajrbahr.mediatork.MediatorBuilder
import com.fajrbahr.mediatork.sample.university.student.StudentStore

fun MediatorBuilder.aboutModule(store: StudentStore) {
    handle(aboutHandler(store))
}
