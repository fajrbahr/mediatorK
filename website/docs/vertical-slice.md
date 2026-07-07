---
id: vertical-slice
title: Vertical Slice Architecture
sidebar_label: Vertical Slice Architecture
---

# Vertical Slice Architecture

## How it fits together

In a classic layered architecture every layer talks to the layer below it through abstractions:

```
UI
 └─▶ ViewModel
       └─▶ UseCase
             └─▶ Repository (interface)
                   └─▶ RepositoryImpl
                         ├─▶ RemoteDataSource (interface)
                         │     └─▶ RemoteDataSourceImpl  ──▶ Ktor HttpClient  ──▶ API
                         └─▶ LocalDataSource  (interface)
                               └─▶ LocalDataSourceImpl   ──▶ SQLDelight Driver ──▶ DB
```

With MediatorK the ViewModel collapses to a single `mediator.send(...)` call. The Handler owns the full slice and wires
the concrete clients directly:

```
UI
 └─▶ ViewModel  ──send(Request)──▶  Mediator
                                        └─▶ Handler
                                              ├─▶ HttpClient   ──▶ API
                                              └─▶ SqlDriver    ──▶ DB
```

---

## What the layered version costs: Order example

In the layered architecture, a single "place order" feature needs all of these classes:

- `OrderRemoteDataSource` *(interface)*
- `OrderRemoteDataSourceImpl`
- `OrderLocalDataSource` *(interface)*
- `OrderLocalDataSourceImpl`
- `OrderRepository` *(interface)*
- `OrderRepositoryImpl`
- `PlaceOrderUseCase`
- `OrderViewModel`

---

## A single handler replaces all of it

With MediatorK, the same feature is one request plus one handler. The `HttpClient` and `SqlDriver` are injected
directly, with no extra abstraction layers in between:

```kotlin
// The request
data class PlaceOrderCommand(
    val userId: String,
    val items: List<OrderItem>,
) : Request<Order>

// The handler — HttpClient for remote, SqlDriver for local
class PlaceOrderHandler(
    private val httpClient: HttpClient,
    private val sqlDriver: SqlDriver,
) : RequestHandler<PlaceOrderCommand, Order> {

    override suspend fun handle(
        mediator: Mediator,
        requestContext: RequestContext,
        request: PlaceOrderCommand,
    ): Order {
        // confirm stock via remote API
        val confirmed = httpClient.post<List<OrderItem>>("orders/confirm") {
            setBody(request.items)
        }

        // persist locally via SQLDelight
        val db = OrderDatabase(sqlDriver)
        db.orderQueries.insert(
            userId = request.userId,
            items  = confirmed.joinToString(",") { it.id },
        )

        return Order(userId = request.userId, items = confirmed)
    }
}
```

The ViewModel stays thin; it only dispatches:

```kotlin
class OrderViewModel(
    private val mediator: Mediator,
) : ViewModel() {

    val state = MutableStateFlow<OrderUiState>(OrderUiState())

    fun placeOrder(userId: String, items: List<OrderItem>) {
        viewModelScope.launch {
            state.value = state.value.copy(isLoading = true)
            try {
                val order = mediator.send(PlaceOrderCommand(userId, items))
                state.value = OrderUiState(order = order)
            } catch (e: Exception) {
                state.value = OrderUiState(error = e.message)
            }
        }
    }
}
```

Each feature gets its own `Command`/`Query` + `Handler`, with no cross-feature coupling, no shared ViewModel god-class.

---

## Next

→ [Installation](installation.md)
