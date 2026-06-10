plugins {
    kotlin("jvm")
}

group = "io.github.fajrbahr"
version = "0.6.0"

repositories {
    mavenCentral()
}

dependencies {
    // KSP API — version must match the Kotlin version used in the project.
    // Format: <kotlin-version>-<ksp-patch>
    // Update this when bumping the Kotlin version in the root build.gradle.kts.
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.21-1.0.28")

    // Annotations module so the processor can resolve @FallbackChain
    implementation(project(":mediatork-ksp-annotations"))
}

// Publish the processor so KSP can pick it up via the service-loader mechanism
sourceSets {
    main {
        resources.srcDir("src/main/resources")
    }
}
