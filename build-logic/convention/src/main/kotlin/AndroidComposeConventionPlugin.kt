import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        // `buildFeatures` is a property on CommonExtension in AGP 9; only Application/Library
        // extensions re-declare the block form.
        extensions.configure<CommonExtension> {
            buildFeatures.compose = true
        }
    }
}
