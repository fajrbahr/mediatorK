plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
    signing
}

// group and version come from the root gradle.properties — the single source of
// truth for every published module.

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

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

// The processor is exposed to KSP via the service-loader entry in
// src/main/resources/META-INF/services/ (picked up by the default resources dir).

val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "mediatork-ksp-koin"
            from(components["java"])
            artifact(javadocJar)

            pom {
                name.set("MediatorK KSP Koin")
                description.set("KSP processor for MediatorK — generates handler registration and a Koin module for discovered handlers")
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

