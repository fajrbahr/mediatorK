package com.fajrbahr.mediatork.sample.university.student.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StudentDetailUiState(
    val model: StudentDetailModel? = null,
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
            val model = mediator.send(GetStudentQuery(studentId))
            _state.value = StudentDetailUiState(model = model, isLoading = false)
        }
    }

    fun delete() {
        viewModelScope.launch {
            mediator.send(DeleteStudentCommand(studentId))
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
