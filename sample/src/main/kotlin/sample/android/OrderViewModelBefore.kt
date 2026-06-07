package sample.android

import com.fajrbahr.mediatork.Mediator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sample.command.CreateOrderCommand
import sample.notification.OrderCreatedNotification

class OrderViewModel(
    private val mediator: Mediator
) : ViewModel() {

    // SharedFlow for one-time events (e.g., navigation, snackbar)
    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    // StateFlow for UI state
    private val _stateFlow = MutableStateFlow(OrderUiState())
    val stateFlow: StateFlow<OrderUiState> = _stateFlow.asStateFlow()

    fun createOrder(id: String, amount: Double) {
        viewModelScope.launch {
            _stateFlow.update { it.copy(isLoading = true, error = null) }
            val result = runCatching {
                mediator.send(CreateOrderCommand(id, amount))
            }
            result.onSuccess { orderResult ->
                _stateFlow.update { it.copy(isLoading = false, orderResult = orderResult) }
                // ViewModel can also publish a notification if needed
                mediator.publish(
                    OrderCreatedNotification(
                        orderId = id,
                        customerEmail = "user@example.com",
                        "+1234567890",
                        3.0
                    )
                )
                _eventFlow.emit("Order created successfully")
            }.onFailure { error ->
                _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                _eventFlow.emit("Error: ${error.message}")
            }
        }
    }
}