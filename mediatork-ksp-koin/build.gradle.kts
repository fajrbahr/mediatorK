plugins {
    alias(libs.plugins.kotlin.jvm)
}

group = "io.github.fajrbahr"
version = "0.8.1"

repositories {
    mavenCentral()
}

dependencies {
    // KSP API — version must match the Kotlin version used in the project.
    // Format: <kotlin-version>-<ksp-patch>
    // Update this when bumping the Kotlin version in the root build.gradle.kts.
    implementation(libs.ksp.api)
    implementation(project(":mediatork"))
    compileOnly("io.insert-koin:koin-core:3.5.6")
}

// Publish the processor so KSP can pick it up via the service-loader mechanism
sourceSets {
    main {
        resources.srcDir("src/main/resources")
    }
}
