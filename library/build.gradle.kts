plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "io.github.fajrbahr"
version = "0.6.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.coroutines.core)
    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

tasks.test {
    useJUnitPlatform()
}
