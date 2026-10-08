plugins {
    id("dev.barter.android.library")
    id("dev.barter.android.hilt")
}

android {
    namespace = "dev.barter.core.data"
}

// Room writes its table layout here on every build. Commit the files: a future migration is checked
// against them, and they are how an app update can be proven not to lose anyone's items.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // `api`: features depend on core:data but should only ever import domain types.
    api(project(":core:domain"))
    implementation(project(":core:identity"))

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
