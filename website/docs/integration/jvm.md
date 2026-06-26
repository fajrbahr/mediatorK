---
id: jvm
title: Kotlin JVM
sidebar_label: Kotlin JVM
---

# Kotlin JVM

Use MediatorK in any JVM project: CLI tools, plain Kotlin, or as the foundation for framework-specific integrations.

See [Installation](../installation.md) for dependency coordinates.

---

## Basic wiring

```kotlin
fun main() {
    val mediator = MediatorFactory.create(
        registrars = listOf(AppRegistrar()),
        pipelineBehaviors = listOf(LoggingBehavior()),
    )

    runBlocking {
        val user = mediator.send(GetUserQuery("user-1"))
        println(user)
    }
}
```

---

## Next

→ [Kotlin Multiplatform](kmp.md)
