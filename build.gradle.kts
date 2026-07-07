plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.binary.compatibility.validator)
}

// Guards the public API of the published modules: any signature change fails CI
// until it is made explicit by re-running `./gradlew apiDump`.
apiValidation {
    ignoredProjects += listOf("publishMaven")
}

detekt {
    source.setFrom(
        files(
            "mediatork/src",
            "mediatork-test/src",
        ),
    )
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}
