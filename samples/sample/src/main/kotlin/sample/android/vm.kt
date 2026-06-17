package sample.android

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import sample.command.OrderResult

open class ViewModel {
    val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
}

// ---------- UI State ----------
data class OrderUiState(
    val isLoading: Boolean = false,
    val orderResult: OrderResult? = null,
    val error: String? = null
)
