plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.nmcp)
    `maven-publish`
    signing
}

group = "io.github.fajrbahr"
version = "0.6.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":mediatork"))
    implementation(libs.classgraph)
    implementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "mediatork-test"
            from(components["java"])
            artifact(javadocJar)

            pom {
                name.set("MediatorK Test")
                description.set("Test utilities for MediatorK — assert all handlers are registered")
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
