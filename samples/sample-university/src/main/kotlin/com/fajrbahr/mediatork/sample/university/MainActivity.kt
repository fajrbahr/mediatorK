package com.fajrbahr.mediatork.sample.university

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fajrbahr.mediatork.MediatorFactory
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.course.CourseRegistrar
import com.fajrbahr.mediatork.sample.university.course.CourseStore
import com.fajrbahr.mediatork.sample.university.department.DepartmentRegistrar
import com.fajrbahr.mediatork.sample.university.department.DepartmentStore
import com.fajrbahr.mediatork.sample.university.instructor.InstructorRegistrar
import com.fajrbahr.mediatork.sample.university.instructor.InstructorStore
import com.fajrbahr.mediatork.sample.university.student.StudentRegistrar
import com.fajrbahr.mediatork.sample.university.student.StudentStore
import com.fajrbahr.mediatork.sample.university.course.list.CourseListScreen
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseScreen
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseScreen
import com.fajrbahr.mediatork.sample.university.common.UniversityTheme
import com.fajrbahr.mediatork.sample.university.course.detail.CourseDetailScreen
import com.fajrbahr.mediatork.sample.university.department.create.CreateDepartmentScreen
import com.fajrbahr.mediatork.sample.university.department.detail.DepartmentDetailScreen
import com.fajrbahr.mediatork.sample.university.department.list.DepartmentListScreen
import com.fajrbahr.mediatork.sample.university.department.edit.EditDepartmentScreen
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorScreen
import com.fajrbahr.mediatork.sample.university.instructor.detail.InstructorDetailScreen
import com.fajrbahr.mediatork.sample.university.instructor.list.InstructorListScreen
import com.fajrbahr.mediatork.sample.university.student.create.CreateStudentScreen
import com.fajrbahr.mediatork.sample.university.student.edit.EditStudentScreen
import com.fajrbahr.mediatork.sample.university.student.detail.StudentDetailScreen
import com.fajrbahr.mediatork.sample.university.student.list.StudentListScreen
import com.fajrbahr.mediatork.sample.university.course.list.CourseListViewModel
import com.fajrbahr.mediatork.sample.university.course.create.CreateCourseViewModel
import com.fajrbahr.mediatork.sample.university.course.edit.EditCourseViewModel
import com.fajrbahr.mediatork.sample.university.course.detail.CourseDetailViewModel
import com.fajrbahr.mediatork.sample.university.department.create.CreateDepartmentViewModel
import com.fajrbahr.mediatork.sample.university.department.detail.DepartmentDetailViewModel
import com.fajrbahr.mediatork.sample.university.department.list.DepartmentListViewModel
import com.fajrbahr.mediatork.sample.university.department.edit.EditDepartmentViewModel
import com.fajrbahr.mediatork.sample.university.instructor.createedit.CreateEditInstructorViewModel
import com.fajrbahr.mediatork.sample.university.instructor.detail.InstructorDetailViewModel
import com.fajrbahr.mediatork.sample.university.instructor.list.InstructorListViewModel
import com.fajrbahr.mediatork.sample.university.student.create.CreateStudentViewModel
import com.fajrbahr.mediatork.sample.university.student.edit.EditStudentViewModel
import com.fajrbahr.mediatork.sample.university.student.detail.StudentDetailViewModel
import com.fajrbahr.mediatork.sample.university.student.list.StudentListViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UniversityTheme {
                AppRoot()
            }
        }
    }
}

private enum class Screen {
    Home,
    CourseList, CourseCreate, CourseEdit, CourseDetail,
    StudentList, StudentCreate, StudentEdit, StudentDetail,
    DepartmentList, DepartmentCreate, DepartmentEdit, DepartmentDetail,
    InstructorList, InstructorCreate, InstructorEdit, InstructorDetail,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    var screen by rememberSaveable { mutableStateOf(Screen.Home) }
    var editId by rememberSaveable { mutableIntStateOf(0) }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("university", Context.MODE_PRIVATE) }
    val courseStore = remember { CourseStore(prefs) }
    val deptStore = remember { DepartmentStore(prefs) }
    val instructorStore = remember { InstructorStore(prefs) }
    val studentStore = remember { StudentStore(prefs) }
    val mediator: Mediator = remember {
        MediatorFactory.create(
            registrars = listOf(
                CourseRegistrar(courseStore, deptStore, studentStore),
                DepartmentRegistrar(deptStore, instructorStore, courseStore),
                InstructorRegistrar(instructorStore, deptStore, courseStore, studentStore),
                StudentRegistrar(studentStore, courseStore),
            )
        )
    }

    val courseListVm = remember { CourseListViewModel(mediator) }
    val studentListVm = remember { StudentListViewModel(mediator) }
    val deptListVm = remember { DepartmentListViewModel(mediator) }
    val instructorListVm = remember { InstructorListViewModel(mediator) }

    when (screen) {
        Screen.Home -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Contoso University", fontWeight = FontWeight.Bold) },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text("Entities", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            onClick = { screen = Screen.CourseList },
                            modifier = Modifier.weight(1f)
                        ) { Text("Courses") }
                        FilledTonalButton(
                            onClick = { screen = Screen.StudentList },
                            modifier = Modifier.weight(1f)
                        ) { Text("Students") }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(
                            onClick = { screen = Screen.DepartmentList },
                            modifier = Modifier.weight(1f)
                        ) { Text("Departments") }
                        FilledTonalButton(
                            onClick = { screen = Screen.InstructorList },
                            modifier = Modifier.weight(1f)
                        ) { Text("Instructors") }
                    }
                }
            }
        }

        // --- Course ---
        Screen.CourseList -> CourseListScreen(
            viewModel = courseListVm,
            onCreateClick = { screen = Screen.CourseCreate },
            onEditClick = { editId = it; screen = Screen.CourseEdit },
        )

        Screen.CourseCreate -> {
            val vm = remember { CreateCourseViewModel(mediator) }
            CreateCourseScreen(vm) { courseListVm.load(); screen = Screen.CourseList }
        }

        Screen.CourseEdit -> {
            val vm = remember(editId) { EditCourseViewModel(mediator, editId) }
            EditCourseScreen(vm) { courseListVm.load(); screen = Screen.CourseList }
        }

        Screen.CourseDetail -> {
            val vm = remember(editId) { CourseDetailViewModel(mediator, editId) }
            CourseDetailScreen(vm) { courseListVm.load(); screen = Screen.CourseList }
        }

        // --- Student ---
        Screen.StudentList -> StudentListScreen(
            viewModel = studentListVm,
            onCreateClick = { screen = Screen.StudentCreate },
            onEditClick = { editId = it; screen = Screen.StudentEdit },
            onDetailsClick = { editId = it; screen = Screen.StudentDetail },
        )

        Screen.StudentCreate -> {
            val vm = remember { CreateStudentViewModel(mediator) }
            CreateStudentScreen(vm) { studentListVm.load(); screen = Screen.StudentList }
        }

        Screen.StudentEdit -> {
            val vm = remember(editId) { EditStudentViewModel(mediator, editId) }
            EditStudentScreen(vm) { studentListVm.load(); screen = Screen.StudentList }
        }

        Screen.StudentDetail -> {
            val vm = remember(editId) { StudentDetailViewModel(mediator, editId) }
            StudentDetailScreen(vm) { studentListVm.load(); screen = Screen.StudentList }
        }

        // --- Department ---
        Screen.DepartmentList -> DepartmentListScreen(
            viewModel = deptListVm,
            onCreateClick = { screen = Screen.DepartmentCreate },
            onEditClick = { editId = it; screen = Screen.DepartmentEdit },
            onDetailsClick = { editId = it; screen = Screen.DepartmentDetail },
        )

        Screen.DepartmentCreate -> {
            val vm = remember { CreateDepartmentViewModel(mediator) }
            CreateDepartmentScreen(vm) { deptListVm.load(); screen = Screen.DepartmentList }
        }

        Screen.DepartmentEdit -> {
            val vm = remember(editId) { EditDepartmentViewModel(mediator, editId) }
            EditDepartmentScreen(vm) { deptListVm.load(); screen = Screen.DepartmentList }
        }

        Screen.DepartmentDetail -> {
            val vm = remember(editId) { DepartmentDetailViewModel(mediator, editId) }
            DepartmentDetailScreen(vm) { deptListVm.load(); screen = Screen.DepartmentList }
        }

        // --- Instructor ---
        Screen.InstructorList -> InstructorListScreen(
            viewModel = instructorListVm,
            onCreateClick = { screen = Screen.InstructorCreate },
            onEditClick = { editId = it; screen = Screen.InstructorEdit },
            onDetailsClick = { editId = it; screen = Screen.InstructorDetail },
        )

        Screen.InstructorCreate -> {
            val vm = remember { CreateEditInstructorViewModel(mediator) }
            CreateEditInstructorScreen(vm) { instructorListVm.load(); screen = Screen.InstructorList }
        }

        Screen.InstructorEdit -> {
            val vm = remember(editId) { CreateEditInstructorViewModel(mediator, editId) }
            CreateEditInstructorScreen(vm) { instructorListVm.load(); screen = Screen.InstructorList }
        }

        Screen.InstructorDetail -> {
            val vm = remember(editId) { InstructorDetailViewModel(mediator, editId) }
            InstructorDetailScreen(vm) { instructorListVm.load(); screen = Screen.InstructorList }
        }
    }
}
