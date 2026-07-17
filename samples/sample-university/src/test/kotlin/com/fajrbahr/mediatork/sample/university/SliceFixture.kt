package com.fajrbahr.mediatork.sample.university

import android.content.SharedPreferences
import com.fajrbahr.mediatork.mediatorK
import com.fajrbahr.mediatork.sample.university.about.aboutModule
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.course.courseModule
import com.fajrbahr.mediatork.sample.university.course.model.Course
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.department.departmentModule
import com.fajrbahr.mediatork.sample.university.department.model.Department
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore
import com.fajrbahr.mediatork.sample.university.instructor.instructorModule
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorCommand
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor
import com.fajrbahr.mediatork.sample.university.model.Enrollment
import com.fajrbahr.mediatork.sample.university.model.Grade
import com.fajrbahr.mediatork.sample.university.student.StudentStore
import com.fajrbahr.mediatork.sample.university.student.studentModule
import com.fajrbahr.mediatork.sample.university.student.create.CreateStudentCommand
import com.fajrbahr.mediatork.sample.university.student.model.Student

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

    val harness =
        mediatorK {
            courseModule(courseStore, deptStore, studentStore)
            departmentModule(deptStore, instructorStore, courseStore)
            instructorModule(instructorStore, deptStore, courseStore, studentStore)
            studentModule(studentStore, courseStore)
            aboutModule(studentStore)
        }

    fun nextCourseNumber(): Int = ++courseNumberSeq

    // ── Back-door inserts (bypass mediator, write directly to store) ────

    fun insertDepartment(
        name: String = "Engineering",
        budget: Double = 123.0,
        startDate: String = "2024-01-01",
        administratorId: Int? = null,
    ): Int {
        val id = deptStore.nextId()
        deptStore.save(Department(id = id, name = name, budget = budget, startDate = startDate, administratorId = administratorId))
        return id
    }

    fun insertCourse(
        title: String = "Chemistry",
        credits: Int = 4,
        departmentId: Int,
        number: Int = nextCourseNumber(),
    ): Int {
        val id = courseStore.nextId()
        courseStore.save(Course(id = id, number = number, title = title, credits = credits, departmentId = departmentId))
        return id
    }

    fun insertStudent(
        lastName: String = "Schmoe",
        firstMidName: String = "Joe",
        enrollmentDate: String = "2024-01-01",
    ): Int {
        val id = studentStore.nextId()
        studentStore.save(Student(id = id, lastName = lastName, firstMidName = firstMidName, enrollmentDate = enrollmentDate))
        return id
    }

    fun insertEnrollment(
        courseId: Int,
        studentId: Int,
        grade: Grade? = null,
    ): Int {
        val id = studentStore.nextEnrollmentId()
        studentStore.saveEnrollment(Enrollment(id = id, courseId = courseId, studentId = studentId, grade = grade))
        return id
    }

    // ── Back-door finds (bypass mediator, read directly from store) ─────

    fun findCourse(id: Int): Course? = courseStore.findById(id)

    fun findDepartment(id: Int): Department? = deptStore.findById(id)

    fun findInstructor(id: Int): Instructor? = instructorStore.findById(id)

    fun findStudent(id: Int): Student? = studentStore.findById(id)

    // ── Front-door helpers (through mediator) ───────────────────────────

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
}
