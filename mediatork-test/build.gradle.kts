plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.nmcp)
    `maven-publish`
    signing
}

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())

    android {
        namespace = "com.fajrbahr.mediatork.test"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }

    androidNativeX64()
    androidNativeX86()
    androidNativeArm32()
    androidNativeArm64()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    js {
        browser()
    }

    jvm()

    linuxArm64()
    linuxX64()

    macosArm64()

    mingwX64()

    tvosArm64()
    tvosSimulatorArm64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            // Core types (Mediator, HandlerRegistry, …) appear in this module's
            // public API, and the assertion helpers surface kotlin.test at call
            // sites via inline functions — both must be `api`.
            api(project(":mediatork"))
            api(kotlin("test"))
            implementation(libs.coroutines.core)
        }
        jvmMain.dependencies {
            // Classpath scanning for MediatorTestUtils.assertAllHandlersRegistered (JVM-only).
            implementation(libs.classgraph)
        }
        commonTest.dependencies {
            implementation(libs.coroutines.test)
        }
    }
}

