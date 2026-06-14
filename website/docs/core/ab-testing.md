---
id: ab-testing
title: A/B Testing & Feature Flags
sidebar_label: A/B Testing
---

# A/B Testing & Feature Flags

MediatorK makes A/B testing straightforward — define one command per variant, register a handler for each, then let the
caller pick which command to send based on a feature flag. The mediator and handlers stay completely unaware of each
other.

---

## How it works

Each variant is its own command that shares the same response type. The flag decision happens at the call site — before
`send()` is called.

```kotlin
// Two variants — same response type, different implementations
data class StripePaymentCommand(val orderId: String, val amount: Double) : Request<PaymentResult>
data class NetworkPaymentCommand(val orderId: String, val amount: Double) : Request<PaymentResult>
```

Register both handlers at startup:

```kotlin
class PaymentRegistrar : MediatorRegistrar {
    override fun register(registry: HandlerRegistry) {
        registry.scope {
            +StripePaymentHandler()
            +NetworkPaymentHandler()
        }
    }
}
```

At the call site, fetch the flag and send the matching command:

```kotlin
val useStripe = featureFlags.isEnabled("stripe_checkout")

val result = mediator.send(
    if (useStripe) StripePaymentCommand(orderId, amount)
    else           NetworkPaymentCommand(orderId, amount)
)
```

---

## Android ViewModel example

```kotlin
class CheckoutViewModel(
    private val mediator: Mediator,
    private val featureFlags: FeatureFlagService,
) : ViewModel() {

    val paymentResult = MutableStateFlow<PaymentResult?>(null)

    fun pay(orderId: String, amount: Double) {
        viewModelScope.launch {
            val useStripe = featureFlags.isEnabled("stripe_checkout")

            paymentResult.value = mediator.send(
                if (useStripe) StripePaymentCommand(orderId, amount)
                else           NetworkPaymentCommand(orderId, amount)
            )
        }
    }
}
```

---

## Why this works well

|                          | Detail                                                                   |
|--------------------------|--------------------------------------------------------------------------|
| No coupling              | Handlers know nothing about flags or each other                          |
| Easy to clean up         | When the experiment ends, delete one command + one handler               |
| Testable                 | Test each variant independently by sending its command directly          |
| Pipeline applies to both | `LoggingBehavior`, `AuthBehavior`, etc. wrap both variants automatically |

---

## Next

→ [Pipeline Behaviors](pipeline.md) — add cross-cutting concerns like logging and auth that apply to all variants
