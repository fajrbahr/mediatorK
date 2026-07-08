package com.fajrbahr.mediatork.sample.university

import android.content.SharedPreferences
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.test.HandlerTestHarness
import com.fajrbahr.mediatork.sample.university.course.CourseRegistrar
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseCommand
import com.fajrbahr.mediatork.sample.university.department.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.department.create.CreateDepartmentCommand
import com.fajrbahr.mediatork.sample.university.instructor.InstructorRegistrar
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorCommand
import com.fajrbahr.mediatork.sample.university.student.StudentRegistrar
import com.fajrbahr.mediatork.sample.university.student.StudentStore
import com.fajrbahr.mediatork.sample.university.student.create.CreateStudentCommand

class InMemorySharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any?>()
    private val editor = object : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        override fun putString(key: String, value: String?) = apply { pending[key] = value }
        override fun putInt(key: String, value: Int) = apply { pending[key] = value }
        override fun putLong(key: String, value: Long) = apply { pending[key] = value }
        override fun putFloat(key: String, value: Float) = apply { pending[key] = value }
        override fun putBoolean(key: String, value: Boolean) = apply { pending[key] = value }
        override fun putStringSet(key: String, values: MutableSet<String>?) = apply { pending[key] = values }
        override fun remove(key: String) = apply { removals.add(key) }
        override fun clear() = apply { data.clear() }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            removals.forEach { data.remove(it) }
            data.putAll(pending)
            pending.clear()
            removals.clear()
        }
    }

    override fun getAll(): MutableMap<String, *> = data.toMutableMap()
    override fun getString(key: String, defValue: String?): String? = data[key] as? String ?: defValue
    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String>) ?: defValues
    override fun getInt(key: String, defValue: Int): Int = data[key] as? Int ?: defValue
    override fun getLong(key: String, defValue: Long): Long = data[key] as? Long ?: defValue
    override fun getFloat(key: String, defValue: Float): Float = data[key] as? Float ?: defValue
    override fun getBoolean(key: String, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
    override fun contains(key: String): Boolean = data.containsKey(key)
    override fun edit(): SharedPreferences.Editor = editor
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
}

class SliceFixture {

    private val prefs = InMemorySharedPreferences()
    private val courseStore = CourseStore(prefs)
    private val deptStore = DepartmentStore(prefs)
    private val instructorStore = InstructorStore(prefs)
    private val studentStore = StudentStore(prefs)
    private var courseNumberSeq = 1000

    val harness: HandlerTestHarness = HandlerTestHarness(
        MediatorFactory.create(
            registrars = listOf(
                CourseRegistrar(courseStore, deptStore),
                DepartmentRegistrar(deptStore, instructorStore),
                InstructorRegistrar(instructorStore, deptStore),
                StudentRegistrar(studentStore),
            ),
        )
    )

    fun nextCourseNumber(): Int = ++courseNumberSeq

    suspend fun createDepartment(
        name: String = "Engineering",
        budget: Double = 100.0,
        startDate: String = "2024-01-01",
        administratorId: Int? = null,
    ): Int = harness.send(
        CreateDepartmentCommand(
            name = name, budget = budget, startDate = startDate, administratorId = administratorId,
        )
    )

    suspend fun createInstructor(
        lastName: String = "Costanza",
        firstMidName: String = "George",
        hireDate: String = "2024-01-01",
        officeLocation: String? = null,
        selectedCourseIds: List<Int> = emptyList(),
    ): Int = harness.send(
        CreateEditInstructorCommand(
            lastName = lastName, firstMidName = firstMidName, hireDate = hireDate,
            officeLocation = officeLocation, selectedCourseIds = selectedCourseIds,
        )
    )

    suspend fun createStudent(
        lastName: String = "Schmoe",
        firstMidName: String = "Joe",
        enrollmentDate: String = "2024-01-01",
    ): Int = harness.send(
        CreateStudentCommand(
            lastName = lastName, firstMidName = firstMidName, enrollmentDate = enrollmentDate,
        )
    )

    suspend fun createCourse(
        title: String = "Chemistry",
        credits: Int = 3,
        departmentId: Int,
        number: Int = nextCourseNumber(),
    ): Int = harness.send(
        CreateCourseCommand(
            number = number, title = title, credits = credits, departmentId = departmentId,
        )
    )
}
