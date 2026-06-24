package sample.android

import com.fajrbahr.mediatork.api.Mediator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import sample.orders.commands.createorder.CreateOrderCommand
import sample.orders.commands.createorder.OrderResult

data class OrderUiState(
    val orderResult: OrderResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Pure-Kotlin ViewModel that dispatches all actions through a [Mediator].
 * Demonstrates the "one dependency" pattern from the MediatorK promise.
 */
class OrderViewModel(private val mediator: Mediator) {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _stateFlow = MutableStateFlow(OrderUiState())
    val stateFlow: StateFlow<OrderUiState> = _stateFlow.asStateFlow()

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: Flow<String> = _eventFlow.asSharedFlow()

    fun createOrder(id: String, amount: Double) {
        scope.launch {
            _stateFlow.update { it.copy(isLoading = true, error = null) }
            try {
                val result = mediator.send(CreateOrderCommand(id = id, amount = amount))
                _stateFlow.update { it.copy(orderResult = result, isLoading = false) }
                _eventFlow.emit("Order ${result.orderId} created successfully")
            } catch (e: Exception) {
                _stateFlow.update { it.copy(error = e.message, isLoading = false) }
                _eventFlow.emit("Error: ${e.message}")
            }
        }
    }
}
