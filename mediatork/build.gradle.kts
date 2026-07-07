plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.nmcp)
    `maven-publish`
    signing
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

group = "io.github.fajrbahr"
version = "0.8.1"

repositories {
    mavenCentral()
    google()
}

android {
    namespace = "com.fajrbahr.mediatork"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())

    androidTarget {
        publishLibraryVariants("release")
    }

    androidNativeX64()
    androidNativeX86()
    androidNativeArm32()
    androidNativeArm64()

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    js {
        browser()
    }

    jvm()

    linuxArm64()
    linuxX64()

    macosArm64()

    mingwX64()

    tvosArm64()
    tvosSimulatorArm64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    watchosArm32()
    watchosArm64()
    watchosDeviceArm64()
    watchosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
    }
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications.withType<MavenPublication> {
        artifact(javadocJar)

        pom {
            name.set("MediatorK")
            description.set("Kotlin Multiplatform mediator library")
            url.set("https://github.com/fajrbahr/MediatorK")

            licenses {
                license {
                    name.set("CC0-1.0")
                    url.set("https://creativecommons.org/publicdomain/zero/1.0/")
                }
            }

            developers {
                developer {
                    id.set("fajrbahr")
                    name.set("FajrBahr")
                }
            }

            scm {
                connection.set("scm:git:git://github.com/fajrbahr/MediatorK.git")
                developerConnection.set("scm:git:ssh://github.com/fajrbahr/MediatorK.git")
                url.set("https://github.com/fajrbahr/MediatorK")
            }
        }
    }
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    if (signingKey != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications)
    }
}

tasks.withType<AbstractPublishToMaven>().configureEach {
    mustRunAfter(tasks.withType<Sign>())
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username = providers.gradleProperty("mavenCentralUsername").orElse("")
        password = providers.gradleProperty("mavenCentralPassword").orElse("")
        publishingType = "AUTOMATIC"
    }
}
