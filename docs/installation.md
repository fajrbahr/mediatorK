# Installation

## Kotlin JVM

For Spring Boot, Ktor, or any plain JVM project.

**Gradle (Kotlin DSL)**
```kotlin
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.1.6")
}
```

**Gradle (Groovy)**
```groovy
dependencies {
    implementation 'io.github.fajrbahr:mediatork:0.1.6'
}
```

**Maven**

> Maven does not resolve Kotlin Multiplatform metadata — use the `-jvm` artifact ID.

```xml
<dependency>
    <groupId>io.github.fajrbahr</groupId>
    <artifactId>mediatork-jvm</artifactId>
    <version>0.1.6</version>
</dependency>
```

---

## Android

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.1.6")
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
            implementation("io.github.fajrbahr:mediatork:0.1.6")
        }
    }
}
```

**Supported targets**

| Target | Platform |
|---|---|
| `jvm` | JVM / Spring Boot / Ktor |
| `androidTarget` | Android |
| `iosArm64` | iOS device |
| `iosSimulatorArm64` | iOS Simulator (Apple Silicon) |
| `iosX64` | iOS Simulator (Intel) |

---

## Testing utilities

```kotlin
dependencies {
    testImplementation("io.github.fajrbahr:mediatork-test:0.1.6")
}
```

See [Testing](testing.md) for usage.

---

## Requirements

| Requirement | Version |
|---|---|
| Kotlin | 2.0+ |
| kotlinx-coroutines | 1.10+ |
| JVM target | 11+ |
