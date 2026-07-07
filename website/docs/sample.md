---
id: sample
title: Samples
sidebar_label: Samples
---

# Samples

:::tip How to run
Each sample is a standalone Gradle project. Open the sample folder directly in IntelliJ IDEA (**File → Open** → select the sample directory) — do not open the root MediatorK project.
:::

## Basic

[`/samples/basic`](https://github.com/fajrbahr/MediatorK/tree/main/samples/basic)

Smallest end-to-end demo: a Todo domain with a command, a query, and a notification. No framework overhead.

---

## Android: University Management

[`/samples/sample-university`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-university)

Jetpack Compose app modeling a university domain (courses, departments, instructors, students). Shows vertical slice architecture with multiple feature slices, validators, handlers, and domain models.

---

## Ktor: Prayer Times (Vertical Slice)

[`/samples/sample-ktor`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-ktor)

Ktor (Netty) HTTP server fetching prayer times and Islamic month data from the Aladhan API. Pure vertical slice architecture: each feature (prayer times, Islamic months) is a self-contained slice with its own handler, model, and registrar.

---

## Spring Boot: Prayer Times (Vertical Slice)

[`/samples/sample-spring`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-spring)

Spring Boot WebFlux REST API with the same features as sample-ktor. Demonstrates vertical slices with Spring component scanning and dependency injection — each feature owns its request, handler, registrar, and controller.

---

## Kotlin/JVM: Full Showcase

[`/samples/sample`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample)

Plain JVM sample covering commands, queries, notifications, pipeline behaviors, and a complete vertical invoice slice with streaming, validation, and transaction support.

---

## Next

→ [Vertical Slice Architecture](vertical-slice.md)
