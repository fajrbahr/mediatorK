package com.fajrbahr.mediatork.sample.university.department.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DepartmentDetailUiState(
    val model: DepartmentDetailModel? = null,
    val isLoading: Boolean = true,
)

class DepartmentDetailViewModel(
    private val mediator: Mediator,
    private val departmentId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(DepartmentDetailUiState())
    val state: StateFlow<DepartmentDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val model = mediator.send(GetDepartmentQuery(departmentId))
            _state.value = DepartmentDetailUiState(model = model, isLoading = false)
        }
    }

}
