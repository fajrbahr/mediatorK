pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "MediatorK"

include("library")
project(":library").name = "mediatork"
include("mediatork-test")

include("publishMaven")

include("mediatork-ksp-koin")


dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

