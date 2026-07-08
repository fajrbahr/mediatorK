package com.fajrbahr.mediatork.sample.university.instructor.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.instructor.model.Instructor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class InstructorDetailUiState(
    val instructor: Instructor? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
)

class InstructorDetailViewModel(
    private val mediator: Mediator,
    private val instructorId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(InstructorDetailUiState())
    val state: StateFlow<InstructorDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val instructor = mediator.send(GetInstructorQuery(instructorId))
            _state.value = InstructorDetailUiState(instructor = instructor, isLoading = false)
        }
    }

    fun delete() {
        viewModelScope.launch {
            mediator.send(DeleteInstructorCommand(instructorId))
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
