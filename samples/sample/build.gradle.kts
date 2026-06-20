plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "com.fajrbahr.mediatork"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
    testImplementation(project(":mediatork-test"))
    implementation(project(":mediatork"))
    implementation(libs.coroutines.core)

}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

tasks.test {
    useJUnitPlatform()
}