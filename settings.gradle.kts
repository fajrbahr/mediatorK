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

include("mediatork")
include("mediatork-test")
include("publishMaven")
include("local-sample")
include("samples:basic")
include("samples:sample")
include("samples:sample-university")

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

