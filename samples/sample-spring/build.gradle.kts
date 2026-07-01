plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.spring)
}

group = "com.fajrbahr.mediatork"
version = "1.0-SNAPSHOT"

dependencies {
    implementation("io.github.fajrbahr:mediatork:0.6.3")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.2")
    implementation(libs.coroutines.core)
    implementation("org.json:json:20240303")
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}
