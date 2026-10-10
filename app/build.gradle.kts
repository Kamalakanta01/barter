plugins {
    id("dev.barter.android.application")
    id("dev.barter.android.compose")
    id("dev.barter.android.hilt")
}

android {
    namespace = "dev.barter.app"

    defaultConfig {
        applicationId = "dev.barter.app"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    // core:data api-exposes core:domain, so this one dependency supplies both the Hilt bindings and
    // the repository interface. Features are added here as they are created.
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:browse"))
    implementation(project(":feature:myitems"))
    implementation(project(":feature:offers"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    // Installs the baseline profile (src/main/baseline-prof.txt) on devices that did not get it from the
    // store, so a side-loaded build starts as fast as a store one.
    implementation(libs.androidx.profileinstaller)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
