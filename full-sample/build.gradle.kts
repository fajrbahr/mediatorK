plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("sample.meditor.MainKt")
}

dependencies {
    implementation(project(":mediatork"))
    implementation(libs.coroutines.core)

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

tasks.test {
    useJUnitPlatform()
}
