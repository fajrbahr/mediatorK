# Installation

## Kotlin JVM

For Spring Boot, Ktor, or any plain JVM project.

**Gradle (Kotlin DSL)**
```kotlin
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.0.5")
}
```

**Gradle (Groovy)**
```groovy
dependencies {
    implementation 'io.github.fajrbahr:mediatork:0.0.5'
}
```

**Maven**

> Maven does not resolve Kotlin Multiplatform metadata — use the `-jvm` artifact ID.

```xml
<dependency>
    <groupId>io.github.fajrbahr</groupId>
    <artifactId>mediatork-jvm</artifactId>
    <version>0.0.5</version>
</dependency>
```

---

## Android

```kotlin
// app/build.gradle.kts
dependencies {
    implementation("io.github.fajrbahr:mediatork:0.0.5")
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
            implementation("io.github.fajrbahr:mediatork:0.0.5")
        }
    }
}
```

---

## Requirements

| Requirement | Version |
|---|---|
| Kotlin | 2.0+ |
| kotlinx-coroutines | 1.10+ |
| JVM target | 11+ |
