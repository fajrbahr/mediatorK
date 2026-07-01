package com.fajrbahr.mediatork.sample.university.department.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fajrbahr.mediatork.api.Mediator
import com.fajrbahr.mediatork.sample.university.department.domain.CreateDepartmentCommand
import com.fajrbahr.mediatork.validator.ValidationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreateDepartmentUiState(
    val name: String = "",
    val budget: String = "",
    val startDate: String = "",
    val administratorId: String = "",
    val errors: List<String> = emptyList(),
    val isSaved: Boolean = false,
)

class CreateDepartmentViewModel(private val mediator: Mediator) : ViewModel() {

    private val _state = MutableStateFlow(CreateDepartmentUiState())
    val state: StateFlow<CreateDepartmentUiState> = _state.asStateFlow()

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
                    CreateDepartmentCommand(
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
