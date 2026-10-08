plugins {
    id("dev.barter.android.library")
    id("dev.barter.android.compose")
    id("dev.barter.android.hilt")
}

android {
    namespace = "dev.barter.feature.offers"
}

dependencies {
    api(project(":core:domain"))
    testImplementation(testFixtures(project(":core:domain")))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.compose.ui.tooling.preview)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
