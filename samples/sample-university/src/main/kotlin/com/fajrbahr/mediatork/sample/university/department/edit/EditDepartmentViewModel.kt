package com.fajrbahr.mediatork.sample.university.department.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.department.domain.EditDepartmentCommand
import com.fajrbahr.mediatork.sample.university.department.domain.GetDepartmentQuery
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditDepartmentUiState(
    val id: Int = 0,
    val name: String = "",
    val budget: String = "",
    val startDate: String = "",
    val administratorId: String = "",
    val errors: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
)

class EditDepartmentViewModel(
    private val mediator: Mediator,
    private val departmentId: Int,
) : ViewModel() {

    private val _state = MutableStateFlow(EditDepartmentUiState())
    val state: StateFlow<EditDepartmentUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val dept = mediator.send(GetDepartmentQuery(departmentId))
            if (dept != null) {
                _state.value = EditDepartmentUiState(
                    id = dept.id,
                    name = dept.name,
                    budget = dept.budget.toString(),
                    startDate = dept.startDate,
                    administratorId = dept.administratorId?.toString() ?: "",
                    isLoading = false,
                )
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value, errors = emptyList()) }
    fun onBudgetChange(value: String) = _state.update { it.copy(budget = value, errors = emptyList()) }
    fun onStartDateChange(value: String) = _state.update { it.copy(startDate = value, errors = emptyList()) }
    fun onAdministratorIdChange(value: String) =
        _state.update { it.copy(administratorId = value, errors = emptyList()) }

    fun submit() {
        viewModelScope.launch {
            val s = _state.value
            try {
                mediator.send(
                    EditDepartmentCommand(
                        id = s.id,
                        name = s.name,
                        budget = s.budget.toDoubleOrNull() ?: 0.0,
                        startDate = s.startDate,
                        administratorId = s.administratorId.toIntOrNull(),
                    )
                )
                _state.update { it.copy(isSaved = true) }
            } catch (e: ValidationException) {
                _state.update { it.copy(errors = e.errors.map { err -> err.toString() }) }
            }
        }
    }
}
