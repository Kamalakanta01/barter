plugins {
    id("dev.barter.kotlin.library")
    `java-test-fixtures`
}

dependencies {
    // Exposed to consumers: the repository interface returns Flow types.
    api(libs.kotlinx.coroutines.core)

    testFixturesApi(libs.junit)
    testImplementation(libs.junit)
}
