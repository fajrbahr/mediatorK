package com.fajrbahr.mediatork.sample.university.instructor.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeleteInstructorUiState(
    val model: DeleteInstructorCommand? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
)

class DeleteInstructorViewModel(
    private val mediator: Mediator,
    private val instructorId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteInstructorUiState())
    val state: StateFlow<DeleteInstructorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val model = mediator.send(DeleteInstructorQuery(instructorId))
            _state.value = DeleteInstructorUiState(model = model, isLoading = false)
        }
    }

    fun delete() {
        val model = _state.value.model ?: return
        viewModelScope.launch {
            mediator.send(model)
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
