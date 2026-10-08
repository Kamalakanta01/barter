import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.assign

/**
 * AGP 9 fails a `Test` task that discovers no tests, even when the module has no test source set at
 * all. The design system and the app shell legitimately have no unit tests, so opt out and rely on
 * the modules that do have tests failing for real.
 */
internal fun Project.lenientTestDiscovery() {
    tasks.withType(Test::class.java).configureEach {
        failOnNoDiscoveredTests = false
    }
}
