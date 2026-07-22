plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.library) apply false
    // Load the publish plugin in the root scope so its shared MavenCentralBuildService
    // is loaded once and reused by every module. Applying it only per-module makes each
    // subproject load its own copy, which collides when more than one module publishes
    // to Maven Central (Gradle 9.3 classloader scoping).
    alias(libs.plugins.vanniktech.maven.publish) apply false
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
