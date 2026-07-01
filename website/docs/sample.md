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

## Android: Prayer Times

[`/samples/sample-android`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-android)

Jetpack Compose app fetching prayer times from the Aladhan API. Shows the before / after / after super progression across three layers.

---

## Ktor: Prayer Times

[`/samples/sample-ktor`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-ktor)

Ktor (Netty) HTTP server with the same before / after / after super structure as the Android sample.

---

## Spring Boot: Prayer Times

[`/samples/sample-spring`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample-spring)

Spring Boot WebFlux application with the same three-layer progression, exposing prayer times over REST.

---

## Kotlin/JVM: Full Showcase

[`/samples/sample`](https://github.com/fajrbahr/MediatorK/tree/main/samples/sample)

Plain JVM sample covering commands, queries, notifications, pipeline behaviors, and a complete vertical invoice slice with streaming, validation, and transaction support.

---

## Next

→ [Life Before MediatorK](testing/before-mediator.md)
