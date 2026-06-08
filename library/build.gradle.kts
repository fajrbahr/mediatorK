plugins {
    kotlin("multiplatform")
    `maven-publish`
    signing
    id("com.gradleup.nmcp") version "1.5.0"
}

group = "io.github.fajrbahr"
version = "0.1.2"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(21)

    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
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
            description.set("Kotlin Mediator library – coroutine-first CQRS and Vertical Slice Architecture")
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

// Ensure all publish tasks run after all sign tasks (KMP implicit dependency fix)
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
