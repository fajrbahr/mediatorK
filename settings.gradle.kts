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
include("sample")

include("library")
project(":library").name = "mediatork"
include("mediatork-test")
include("publishMaven")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

