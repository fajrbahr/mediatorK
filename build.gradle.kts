plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.binary.compatibility.validator)
}

// Guards the public API of the published modules: any signature change fails CI
// until it is made explicit by re-running `./gradlew apiDump`.
apiValidation {
    ignoredProjects += listOf("publishMaven", "full-sample", "dsl-sample")
}

detekt {
    source.setFrom(
        files(
            "mediatork/src",
        ),
    )
    buildUponDefaultConfig = true
    baseline = file("detekt-baseline.xml")
}
