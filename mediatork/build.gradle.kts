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

    iosArm64()
    iosSimulatorArm64()
    iosX64()

    js {
        browser()
        nodejs()
    }

    jvm()

    macosX64()
    macosArm64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

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


mavenPublishing {
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
