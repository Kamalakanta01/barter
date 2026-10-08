plugins {
    id("dev.barter.android.library")
    id("dev.barter.android.hilt")
}

android {
    namespace = "dev.barter.core.identity"
}

dependencies {
    api(project(":core:domain"))

    testImplementation(libs.junit)
}
