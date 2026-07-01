package com.fajrbahr.mediatork.sample.university.department.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.department.domain.DeleteDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.domain.GetDepartmentQuery
import com.fajrbahr.mediatork.sample.university.department.model.Department
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DepartmentDetailUiState(
    val department: Department? = null,
    val isLoading: Boolean = true,
    val isDeleted: Boolean = false,
)

class DepartmentDetailViewModel(
    private val mediator: Mediator,
    private val departmentId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(DepartmentDetailUiState())
    val state: StateFlow<DepartmentDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val dept = mediator.send(GetDepartmentQuery(departmentId))
            _state.value = DepartmentDetailUiState(department = dept, isLoading = false)
        }
    }

    fun delete() {
        viewModelScope.launch {
            mediator.send(DeleteDepartmentCommand(departmentId))
            _state.value = _state.value.copy(isDeleted = true)
        }
    }
}
