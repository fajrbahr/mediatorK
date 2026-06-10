plugins {
    kotlin("multiplatform")
}

group = "io.github.fajrbahr"
version = "0.6.0"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    jvm()

    androidNativeX64()
    androidNativeX86()
    androidNativeArm32()
    androidNativeArm64()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    js { browser() }

    linuxArm64()
    linuxX64()

    macosArm64()
    macosX64()

    mingwX64()

    tvosArm64()
    tvosSimulatorArm64()
    tvosX64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs { browser() }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmWasi { nodejs() }

    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()
    watchosX64()
}
