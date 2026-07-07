plugins {
    kotlin("jvm") version "2.3.21"
    id("org.springframework.boot") version "3.3.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jetbrains.kotlin.plugin.spring") version "2.3.21"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.fajrbahr:mediatork:0.8.1")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.json:json:20240303")
}

kotlin {
    jvmToolchain(21)
}
