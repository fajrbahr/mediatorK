plugins {
    kotlin("jvm")
    `maven-publish`
    signing
    id("com.gradleup.nmcp") version "1.5.0"
}

group = "com.fajrbahr.mediatork"
version = "0.0.2"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("release") {
            from(components["java"])
            artifact(javadocJar)

            groupId = "com.fajrbahr.mediatork"
            artifactId = "mediatork"
            version = project.version.toString()

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
}

signing {
    val signingKey: String? by project
    val signingPassword: String? by project
    useInMemoryPgpKeys(signingKey, signingPassword)
    sign(publishing.publications["release"])
}

nmcp {
    publishAllPublicationsToCentralPortal {
        username = providers.gradleProperty("mavenCentralUsername").orElse("")
        password = providers.gradleProperty("mavenCentralPassword").orElse("")
        publishingType = "USER_MANAGED"
    }
}
