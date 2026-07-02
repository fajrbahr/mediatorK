plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

group = "com.fajrbahr.mediatork"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.fajrbahr.mediatork.sample.ktor.ApplicationKt")
}

dependencies {
    implementation(project(":mediatork"))
    implementation("io.ktor:ktor-server-core:3.1.3")
    implementation("io.ktor:ktor-server-netty:3.1.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.json:json:20240303")
    implementation(libs.coroutines.core)
    runtimeOnly("ch.qos.logback:logback-classic:1.4.14")
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}
