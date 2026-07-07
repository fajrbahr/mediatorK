plugins {
    kotlin("jvm") version "2.3.21"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.21"
    application
}

application {
    mainClass.set("com.fajrbahr.mediatork.sample.ktor.ApplicationKt")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.fajrbahr:mediatork:0.8.1")
    implementation("io.ktor:ktor-server-core:3.1.3")
    implementation("io.ktor:ktor-server-netty:3.1.3")
    implementation("io.ktor:ktor-server-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")
    implementation("org.json:json:20240303")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    runtimeOnly("ch.qos.logback:logback-classic:1.4.14")
}

kotlin {
    jvmToolchain(21)
}
