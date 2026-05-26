import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import java.util.concurrent.TimeUnit

plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.wire) apply false
    alias(libs.plugins.detekt)
}

detekt {
    parallel = true
    ignoreFailures = false
    autoCorrect = true
    buildUponDefaultConfig = true
    config.setFrom(rootProject.files("tangem-android-tools/detekt-config.yml"))
    baseline = file("detekt-baseline.xml")
}

tasks.withType<Detekt>().configureEach {
    setSource(file(projectDir))
    include("**/*.kt")
    exclude("**/resources/**", "**/build/**", "**/*.gradle.kts")
    reports {
        sarif.required.set(false)
        txt.required.set(true)
    }
}

tasks.withType<DetektCreateBaselineTask>().configureEach {
    setSource(file(projectDir))
    include("**/*.kt")
    exclude("**/resources/**", "**/build/**", "**/*.gradle.kts")
}

dependencies {
    detektPlugins(libs.detekt.formatting)
    detektPlugins(libs.detekt.compose.rules)
}

gradle.projectsEvaluated {
    val classpathFiles = mutableListOf<Any>()

    rootProject.subprojects.forEach { subproject ->
        val androidExtension = subproject.extensions.findByName("android")
            as? com.android.build.gradle.BaseExtension ?: return@forEach

        classpathFiles.addAll(androidExtension.bootClasspath)

        val variants = when (androidExtension) {
            is com.android.build.gradle.LibraryExtension -> androidExtension.libraryVariants
            is com.android.build.gradle.AppExtension -> androidExtension.applicationVariants
            else -> null
        }

        val debugVariant = variants?.firstOrNull { it.name == "debug" }
        if (debugVariant != null) {
            classpathFiles.add(debugVariant.javaCompileProvider.get().classpath)
        }
    }

    if (classpathFiles.isNotEmpty()) {
        tasks.withType<Detekt>().configureEach {
            jvmTarget = "17"
            classpath.setFrom(files(classpathFiles))
        }
        tasks.withType<DetektCreateBaselineTask>().configureEach {
            jvmTarget = "17"
            classpath.setFrom(files(classpathFiles))
        }
    }
}

configurations.configureEach {
    resolutionStrategy.cacheDynamicVersionsFor(1, TimeUnit.SECONDS)
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}