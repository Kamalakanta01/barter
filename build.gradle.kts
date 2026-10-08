// AGP 9 bundles the Kotlin Gradle plugin (2.2.10) as a runtime dependency. These classpath entries
// raise KGP and KSP to the versions pinned in the version catalog for every module in the build.
// `buildscript` must be the first block in the script.
buildscript {
    dependencies {
        classpath(libs.kotlin.gradlePlugin)
        classpath(libs.ksp.gradlePlugin)
    }
}

plugins {
    // Declared here so plugin versions resolve from one place; nothing is applied to the root project.
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
}
