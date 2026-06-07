plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "MediatorK"
include("sample")

include("library")
include("publishMaven")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

