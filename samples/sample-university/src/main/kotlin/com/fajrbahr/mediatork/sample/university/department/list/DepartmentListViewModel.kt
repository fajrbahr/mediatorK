package com.fajrbahr.mediatork.sample.university.department.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.department.detail.DeleteDepartmentCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DepartmentListUiState {
    data object Loading : DepartmentListUiState
    data class Success(val departments: List<DepartmentListModel>) : DepartmentListUiState
    data class Error(val message: String) : DepartmentListUiState
}

class DepartmentListViewModel(private val mediator: Mediator) : ViewModel() {

    private val _state = MutableStateFlow<DepartmentListUiState>(DepartmentListUiState.Loading)
    val state: StateFlow<DepartmentListUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = DepartmentListUiState.Loading
            runCatching { mediator.send(GetDepartmentsQuery) }
                .fold(
                    onSuccess = { _state.value = DepartmentListUiState.Success(it) },
                    onFailure = { _state.value = DepartmentListUiState.Error(it.message ?: "Unknown error") },
                )
        }
    }

    fun delete(id: Int) {
        viewModelScope.launch {
            mediator.send(DeleteDepartmentCommand(id))
            load()
        }
    }
}
