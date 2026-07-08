package com.fajrbahr.mediatork.sample.university.department.delete

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeleteDepartmentUiState(
    val model: DeleteDepartmentCommand? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
)

class DeleteDepartmentViewModel(
    private val mediator: Mediator,
    private val departmentId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(DeleteDepartmentUiState())
    val state: StateFlow<DeleteDepartmentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val model = mediator.send(DeleteDepartmentQuery(departmentId))
            _state.value = DeleteDepartmentUiState(model = model, isLoading = false)
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
