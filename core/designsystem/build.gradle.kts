plugins {
    id("dev.barter.android.library")
    id("dev.barter.android.compose")
}

android {
    namespace = "dev.barter.core.designsystem"
}

dependencies {
    api(platform(libs.compose.bom))
    api(libs.compose.foundation)
    api(libs.compose.material3)
    api(libs.compose.ui)
    api(libs.compose.ui.graphics)
    api(libs.compose.ui.tooling.preview)

    debugImplementation(libs.compose.ui.tooling)
}
