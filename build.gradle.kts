plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.detekt)
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
