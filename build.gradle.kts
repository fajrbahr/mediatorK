plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.detekt)
    alias(libs.plugins.binary.compatibility.validator)
}

// The nmcp settings plugin applies com.gradleup.nmcp.aggregation to the root
// project. Allow duplicate Gradle project names so the local-samples subprojects
// (which are not published) do not trip the aggregation guard.
nmcpAggregation {
    allowDuplicateProjectNames.set(true)
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
