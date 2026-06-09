---
id: installation
title: Installation
sidebar_label: Installation
---

# Installation

## Kotlin JVM

For Spring Boot, Ktor, or any plain JVM project.

**Gradle (Kotlin DSL)**

```kotlin
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.6.0")
}
```

**Gradle (Groovy)**

```groovy
dependencies {
    implementation 'io.github.fajrbahr:mediatork:0.6.0'
}
```

**Gradle Version Catalog (TOML)**

Add to `gradle/libs.versions.toml`:

```toml
[versions]
mediatork = "0.6.0"

[libraries]
mediatork = { module = "io.github.fajrbahr:mediatork", version.ref = "mediatork" }
```

Then in your `build.gradle.kts`:

```kotlin
dependencies {
    implementation(libs.mediatork)
}
```

**Maven**

:::info
Maven does not resolve Kotlin Multiplatform metadata — use the `-jvm` artifact ID.
:::

```xml
<dependency>
    <groupId>io.github.fajrbahr</groupId>
    <artifactId>mediatork-jvm</artifactId>
    <version>0.6.0</version>
</dependency>
```

---

## Android

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.6.0")
}
```

---

## Kotlin Multiplatform (KMP)

Add to `commonMain` in your shared module. Gradle automatically selects the right platform artifact.

```kotlin
// shared/build.gradle.kts
kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("io.github.fajrbahr:mediatork:0.6.0")
        }
    }
}
```

**Supported targets**

| Target              | Platform                      |
|---------------------|-------------------------------|
| `jvm`               | JVM / Spring Boot / Ktor      |
| `androidTarget`     | Android                       |
| `iosArm64`          | iOS device                    |
| `iosSimulatorArm64` | iOS Simulator (Apple Silicon) |
| `iosX64`            | iOS Simulator (Intel)         |

---

## Testing utilities

```kotlin
dependencies {
    testImplementation("io.github.fajrbahr:mediatork-test:0.6.0")
}
```

See [Handler Validation](testing/handler-validation.md) for usage.

---

## Requirements

| Requirement        | Version |
|--------------------|---------|
| Kotlin             | 2.0+    |
| kotlinx-coroutines | 1.10+   |
| JVM target         | 11+     |
