plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass.set("dsl.meditor.MainKt")
}

dependencies {
    implementation(project(":mediatork"))
    implementation(libs.coroutines.core)

    testImplementation(project(":mediatork-test"))
    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}

kotlin {
    jvmToolchain(libs.versions.jvmToolchain.get().toInt())
}

tasks.test {
    useJUnitPlatform()
}
