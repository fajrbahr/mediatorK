plugins {
    kotlin("jvm")
}

group = "com.fajrbahr.mediatork"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(project(":mediatork"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}