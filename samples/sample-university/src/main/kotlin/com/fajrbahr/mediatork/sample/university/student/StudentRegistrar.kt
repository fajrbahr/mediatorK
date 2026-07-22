package com.fajrbahr.mediatork.sample.university.student

import com.fajrbahr.mediatork.HandlerRegistry
import com.fajrbahr.mediatork.api.MediatorRegistrar
import com.fajrbahr.mediatork.sample.university.about.AboutQueryHandler
import com.fajrbahr.mediatork.sample.university.course.CourseStore
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

class StudentRegistrar(
    private val store: StudentStore,
    private val courseStore: CourseStore,
) : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.registerLazy { AboutQueryHandler(store) }
        registry.registerLazy { GetStudentsHandler(store) }
        registry.registerLazy { GetStudentHandler(store, courseStore) }
        registry.registerLazy { CreateStudentHandler(store) }
        registry.registerValidator(CreateStudentValidator())
        registry.registerLazy { EditStudentQueryHandler(store) }
        registry.registerValidator(EditStudentQueryValidator())
        registry.registerLazy { EditStudentHandler(store) }
        registry.registerValidator(EditStudentValidator())
        registry.registerLazy { DeleteStudentQueryHandler(store) }
        registry.registerValidator(DeleteStudentQueryValidator())
        registry.registerLazy { DeleteStudentHandler(store) }
        registry.registerLazy { EnrollStudentHandler(store) }
    }
}
