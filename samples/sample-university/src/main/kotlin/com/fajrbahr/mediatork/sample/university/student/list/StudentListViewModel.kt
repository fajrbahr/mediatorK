package com.fajrbahr.mediatork.sample.university.student.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.student.detail.DeleteStudentCommand
import com.fajrbahr.mediatork.sample.university.student.model.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StudentListUiState {
    data object Loading : StudentListUiState
    data class Success(val students: List<Student>) : StudentListUiState
    data class Error(val message: String) : StudentListUiState
}

class StudentListViewModel(private val mediator: Mediator) : ViewModel() {

    private val _state = MutableStateFlow<StudentListUiState>(StudentListUiState.Loading)
    val state: StateFlow<StudentListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = StudentListUiState.Loading
            runCatching { mediator.send(GetStudentsQuery) }
                .fold(
                    onSuccess = { _state.value = StudentListUiState.Success(it) },
                    onFailure = { _state.value = StudentListUiState.Error(it.message ?: "Unknown error") },
                )
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            mediator.send(DeleteStudentCommand(id))
            load()
        }
    }
}
