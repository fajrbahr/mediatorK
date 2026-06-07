package sample.android

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import sample.command.OrderResult


// ---------- Data Layer ----------
// DataSource interface and implementation
interface OrderDataSource {
    suspend fun createOrder(id: String, amount: Double): OrderResult
}

class OrderDataSourceImpl : OrderDataSource {
    override suspend fun createOrder(id: String, amount: Double): OrderResult {
        // actual network or database call
        return OrderResult(responseIme = 4)
    }
}

// Repository interface and implementation
interface EmailRepository {
    suspend fun sendOrderConfirmation(orderId: String, customerEmail: String)
}

interface SmsRepository {
    suspend fun sendOrderSms(orderId: String, phoneNumber: String)
}

interface AnalyticsRepository {
    suspend fun trackOrderCreated(orderId: String, amount: Double)
}


interface OrderRepository {
    suspend fun createOrder(id: String, amount: Double): OrderResult
}

class OrderRepositoryImpl(
    private val dataSource: OrderDataSource
) : OrderRepository {
    override suspend fun createOrder(id: String, amount: Double): OrderResult {
        return dataSource.createOrder(id, amount)
    }
}

// ---------- Domain Layer: Use Cases ----------
// ---------- Use Cases ----------
class CreateOrderUseCase(
    private val repository: OrderRepository
) {
    suspend operator fun invoke(id: String, amount: Double): OrderResult {
        return repository.createOrder(id, amount)
    }
}

class SendOrderConfirmationEmailUseCase(
    private val emailRepo: EmailRepository
) {
    suspend operator fun invoke(orderId: String, customerEmail: String) {
        emailRepo.sendOrderConfirmation(orderId, customerEmail)
    }
}

class SendOrderSmsUseCase(
    private val smsRepo: SmsRepository
) {
    suspend operator fun invoke(orderId: String, phoneNumber: String) {
        smsRepo.sendOrderSms(orderId, phoneNumber)
    }
}

class TrackOrderAnalyticsUseCase(
    private val analyticsRepo: AnalyticsRepository
) {
    suspend operator fun invoke(orderId: String, amount: Double) {
        analyticsRepo.trackOrderCreated(orderId, amount)
    }
}

class OrderViewModelBefore(
    private val createOrderUseCase: CreateOrderUseCase,
    private val sendEmailUseCase: SendOrderConfirmationEmailUseCase,
    private val sendSmsUseCase: SendOrderSmsUseCase,
    private val trackAnalyticsUseCase: TrackOrderAnalyticsUseCase
) : ViewModel() {

    private val _eventFlow = MutableSharedFlow<String>()
    val eventFlow: SharedFlow<String> = _eventFlow.asSharedFlow()

    private val _stateFlow = MutableStateFlow(OrderUiState())
    val stateFlow: StateFlow<OrderUiState> = _stateFlow.asStateFlow()

    fun createOrder(id: String, amount: Double, customerEmail: String, phoneNumber: String) {
        viewModelScope.launch {
            _stateFlow.update { it.copy(isLoading = true, error = null) }
            val result = runCatching {
                createOrderUseCase(id, amount)
            }
            result.onSuccess { orderResult ->
                sendEmailUseCase(orderResult.orderId, customerEmail)
                sendSmsUseCase(orderResult.orderId, phoneNumber)
                trackAnalyticsUseCase(orderResult.orderId, amount)

                _stateFlow.update { it.copy(isLoading = false, orderResult = orderResult) }
                _eventFlow.emit("Order created successfully")
            }.onFailure { error ->
                _stateFlow.update { it.copy(isLoading = false, error = error.message) }
                _eventFlow.emit("Error: ${error.message}")
            }
        }
    }
}
