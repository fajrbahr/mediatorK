package com.fajrbahr.mediatork.sample.university.student

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.about.AboutQueryHandler
import com.fajrbahr.mediatork.sample.university.student.create.CreateStudentHandler
import com.fajrbahr.mediatork.sample.university.student.create.CreateStudentValidator
import com.fajrbahr.mediatork.sample.university.student.delete.DeleteStudentHandler
import com.fajrbahr.mediatork.sample.university.student.delete.DeleteStudentQueryHandler
import com.fajrbahr.mediatork.sample.university.student.delete.DeleteStudentQueryValidator
import com.fajrbahr.mediatork.sample.university.student.detail.EnrollStudentHandler
import com.fajrbahr.mediatork.sample.university.student.detail.GetStudentHandler
import com.fajrbahr.mediatork.sample.university.student.edit.EditStudentHandler
import com.fajrbahr.mediatork.sample.university.student.edit.EditStudentQueryHandler
import com.fajrbahr.mediatork.sample.university.student.edit.EditStudentQueryValidator
import com.fajrbahr.mediatork.sample.university.student.edit.EditStudentValidator
import com.fajrbahr.mediatork.sample.university.student.list.GetStudentsHandler

class StudentRegistrar(private val store: StudentStore) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry register AboutQueryHandler(store)
        registry register GetStudentsHandler(store)
        registry register GetStudentHandler(store)
        registry register CreateStudentHandler(store)
        registry.registerValidator(CreateStudentValidator())
        registry register EditStudentQueryHandler(store)
        registry.registerValidator(EditStudentQueryValidator())
        registry register EditStudentHandler(store)
        registry.registerValidator(EditStudentValidator())
        registry register DeleteStudentQueryHandler(store)
        registry.registerValidator(DeleteStudentQueryValidator())
        registry register DeleteStudentHandler(store)
        registry register EnrollStudentHandler(store)
    }
}
