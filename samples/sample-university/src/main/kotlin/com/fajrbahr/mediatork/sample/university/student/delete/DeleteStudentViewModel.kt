package com.fajrbahr.mediatork.sample.university.student.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeleteStudentUiState(
    val model: DeleteStudentCommand? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
)

class DeleteStudentViewModel(
    private val mediator: Mediator,
    private val studentId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteStudentUiState())
    val state: StateFlow<DeleteStudentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val model = mediator.send(DeleteStudentQuery(studentId))
            _state.value = DeleteStudentUiState(model = model, isLoading = false)
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
