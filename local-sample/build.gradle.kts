plugins {
    alias(libs.plugins.kotlin.jvm)
    application
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
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

application {
    mainClass = "local.meditor.MainKt"
}

tasks.test {
    useJUnitPlatform()
}

kover {
    reports {
        // Exclude root-package demo code and Spring stubs from coverage measurement.
        // All real business logic lives in sub-packages (invoice/, behaviors/, android/, …).
        filters {
            excludes {
                classes(
                    "sample.MainKt",
                    "sample.MainKt\$*",
                    "sample.Test*",
                    "sample.AuthenticatedGetOrderQuery",
                    "sample.spring.*",
                    "sample.OrderController",
                )
            }
        }

        verify {
            rule {
                bound {
                    minValue = 66
                }
            }
        }
    }
}
