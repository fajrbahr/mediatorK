# MediatorK

[![Docs](https://img.shields.io/badge/Docs-mediatorK-a97cf8)](https://fajrbahr.github.io/mediatorK/)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.fajrbahr/mediatork?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.fajrbahr/mediatork)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.21-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Gradle](https://img.shields.io/badge/Gradle-9.3.0-02303A.svg?logo=gradle)](https://gradle.org)
[![Android](https://img.shields.io/badge/Android-supported-brightgreen.svg?logo=android)](https://developer.android.com)
[![iOS](https://img.shields.io/badge/iOS-supported-brightgreen.svg?logo=apple)](https://developer.apple.com)
[![Linux](https://img.shields.io/badge/Linux-supported-brightgreen.svg?logo=linux)](https://www.linux.org)
[![Windows](https://img.shields.io/badge/Windows-supported-brightgreen.svg?logo=windows)](https://www.microsoft.com/windows)
[![Web (JS/WASM)](https://img.shields.io/badge/Web%20(JS%2FWASM)-supported-brightgreen.svg?logo=javascript)](https://kotlinlang.org/docs/js-overview.html)
[![CI](https://github.com/fajrbahr/mediatorK/actions/workflows/ci.yml/badge.svg)](https://github.com/fajrbahr/mediatorK/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/fajrbahr/mediatorK/branch/main/graph/badge.svg)](https://codecov.io/gh/fajrbahr/mediatorK)
[![License: CC0](https://img.shields.io/badge/License-CC0-brightgreen)](LICENSE)

A coroutine-first Mediator library for Kotlin. Implements the CQRS and Vertical Slice patterns — requests go to exactly
one handler, notifications fan out to many, and a pipeline of behaviors sits in between.

→ **[Full documentation](https://fajrbahr.github.io/mediatorK/)**

---

## Installation

```kotlin
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.6.3")
}
```

For KMP, Maven, and other project types — see [Installation](https://fajrbahr.github.io/mediatorK/docs/installation).

---

## Quick Start

Define your messages, then wire everything in one block — no registrar or handler classes required:

```kotlin
data class CreateOrderCommand(val id: String, val amount: Double) : Request<Order>
data class OrderCreatedEvent(val orderId: String) : Notification

val mediator = mediatorK {
    handle<CreateOrderCommand, Order> { request ->
        val order = db.save(Order(request.id, request.amount))
        publish(OrderCreatedEvent(order.id))   // handler scope IS the mediator
        order
    }

    on<OrderCreatedEvent> { event -> emailService.send(event.orderId) }

    validate<CreateOrderCommand> { request ->
        rules<String> { check(request.amount > 0) { "Amount must be positive" } }
    }
}

val order = mediator.send(CreateOrderCommand("ORD-1", 150.0))
```

As your project grows, you can structure your DSL registrations using `mediatorRegistrar` blocks to group handlers by feature or domain:

```kotlin
val orderRegistrar = mediatorRegistrar {
    handle<CreateOrderCommand, Order> { request ->
        val order = Order(request.id, request.amount)
        db.save(order)
        publish(OrderCreatedEvent(order.id))
        order
    }

    on<OrderCreatedEvent> { event ->
        emailService.send(event.orderId)
    }
}

val mediator = mediatorK { 
    registrar(orderRegistrar) 
}

val order = mediator.send(CreateOrderCommand("ORD-1", 150.0))
```

---

## Acknowledgements

First and above all — **الحمد لله** (Alhamdulillah). This library was built during a hard time, and every line was
written with Allah's help and guidance.

**[Jimmy Bogard](https://www.jimmybogard.com/)** — for his talks on Vertical Slice Architecture and MediatR (.NET),
which were the direct inspiration for bringing this pattern to Kotlin.

**[beno.com](https://beno.com)** — the production environment that shaped this library. Real-world usage at scale drove
every design decision here.

**Ahmed Akilan**, **Jacqueline Lim**, and **[Jaewoong Eum (skydoves)](https://github.com/skydoves/)** — Ahmed, our CTO,
whose technical mentorship and trust made it possible to grow as an engineer and ship something worth sharing.
Jacqueline, whose support and collaboration were invaluable throughout this journey. And Jaewoong — a one-man
engineering force whose open-source contributions to the Android community are worth a team of 100 engineers.

**[Philipp Lackner](https://www.youtube.com/@PhilippLackner)** — for his Android and Kotlin content on YouTube, which
has been an invaluable learning resource.

**[Dr. Venkat Subramaniam](https://www.agiledeveloper.com/)** — for his exceptional teaching of Kotlin, functional
programming, and software design. His talks and courses shaped the way this library thinks about clean code.

**[droidcon](https://www.droidcon.com/)** — for the talks, conferences, and community that keep Android and Kotlin
engineering moving forward.

---

Released under [CC0 1.0 Universal](LICENSE) — public domain. No attribution required.
