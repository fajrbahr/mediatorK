package com.fajrbahr.mediatork.sample.university.student.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.model.Enrollment
import com.fajrbahr.mediatork.sample.university.student.model.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StudentDetailUiState(
    val student: Student? = null,
    val enrollments: List<Enrollment> = emptyList(),
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
)

class StudentDetailViewModel(
    private val mediator: Mediator,
    private val studentId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(StudentDetailUiState())
    val state: StateFlow<StudentDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val student = mediator.send(GetStudentQuery(studentId))
            val enrollments = mediator.send(GetStudentEnrollmentsQuery(studentId))
            _state.value = StudentDetailUiState(student = student, enrollments = enrollments, isLoading = false)
        }
    }

    fun delete() {
        viewModelScope.launch {
            mediator.send(DeleteStudentCommand(studentId))
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
