@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.maven.publish)
    alias(libs.plugins.dokka)
    id("org.jetbrains.kotlinx.kover") version "0.9.1"
}

group = "io.github.fajrbahr"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())

    androidTarget {
        publishLibraryVariants("release")
    }

    jvm()

    js {
        browser()
        nodejs()
    }

    wasmJs {
        browser {
            testTask {
                enabled = false
            }
        }
        binaries.library()
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    macosArm64()

    sourceSets {
        all {
            languageSettings.optIn("kotlin.contracts.ExperimentalContracts")
        }
        val commonMain by getting {
            dependencies {
                implementation(libs.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.coroutines.test)
            }
        }
    }
}

android {
    namespace = "com.fajrbahr.mediatork"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType(JavaCompile::class.java).configureEach {
    targetCompatibility = JavaVersion.VERSION_11.toString()
    sourceCompatibility = JavaVersion.VERSION_11.toString()
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    pom {
        name.set("MediatorK")
        description.set("Elegant command & request handler pattern with Kotlin multiplatform support")
        url.set("https://github.com/fajrbahr/mediatorK")
        licenses {
            license {
                name.set("CC0-1.0")
                url.set("https://creativecommons.org/publicdomain/zero/1.0/")
            }
        }
        developers {
            developer {
                id.set("fajrbahr")
                name.set("Huzaifa Alfararjeh")
                email.set("huthefa.alfararjeh@beno.com")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/fajrbahr/mediatorK.git")
            developerConnection.set("scm:git:ssh://github.com/fajrbahr/mediatorK.git")
            url.set("https://github.com/fajrbahr/mediatorK")
        }
    }
}

