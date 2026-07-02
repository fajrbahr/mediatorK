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
//include("mediatork-ksp-koin")
include("publishMaven")
include("local-samples:full-sample")
include("local-samples:dsl-sample")


dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

