@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "io.github.fajrbahr"

repositories {
    mavenCentral()
    google()
}

android {
    namespace = "com.fajrbahr.mediatork.test"
    compileSdk = libs.versions.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())

    androidTarget {
        publishLibraryVariants("release")
    }

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    js {
        browser()
        nodejs()
    }

    jvm()

    macosArm64()

    wasmJs {
        browser {
            testTask {
                enabled = false
            }
        }
        binaries.library()
    }

    sourceSets {
        commonMain.dependencies {
            api(project(":mediatork"))
            api(libs.coroutines.core)
            api(kotlin("test"))
        }
        commonTest.dependencies {
            implementation(libs.coroutines.test)
        }
        getByName("jvmMain").dependencies {
            implementation(libs.classgraph)
        }
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
    pom {
        name.set("MediatorK Test")
        description.set("Test utilities for MediatorK – fakes, spies, and harnesses")
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
