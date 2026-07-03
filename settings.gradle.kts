pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("com.gradleup.nmcp.settings") version "1.6.1"
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

nmcpSettings {
    centralPortal {
        username = providers.gradleProperty("mavenCentralUsername").orElse("")
        password = providers.gradleProperty("mavenCentralPassword").orElse("")
        publishingType = "AUTOMATIC"
    }
}

