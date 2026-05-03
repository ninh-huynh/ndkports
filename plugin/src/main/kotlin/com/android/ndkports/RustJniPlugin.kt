package com.android.ndkports

import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import javax.inject.Inject

class RustJniPlugin @Inject constructor() : Plugin<Project> {
    override fun apply(project: Project) {

        project.pluginManager.withPlugin("com.android.library") {
            val androidComponents = project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)

            val extension = project.extensions.create(
                "rustJni",
                RustJniExtension::class.java
            )

            // Register tasks per variant to handle specific minSdk/ABI settings
            androidComponents.onVariants { variant ->

                // Create a unique task name, e.g., buildRustJniLibsDebug
                val taskName = "buildRustJniLibs${variant.name.replaceFirstChar { it.uppercase() }}"

                val buildTask = project.tasks.register(
                    taskName,
                    RustJniPortTask::class.java
                ) {
                    it.sourceDirectory.set(extension.sourceDir)
                    it.libName.set(extension.libName)
                    it.ndkPath.set(androidComponents.sdkComponents.ndkDirectory)
                    it.minSdkVersion.set(variant.minSdk.apiLevel)

                    it.buildType.set(extension.buildType)
                    it.targetAbis.set(extension.targetAbis)
                    // Ensure each variant has its own unique build folder to avoid cache collisions
                    it.buildDir.set(project.layout.buildDirectory.dir("rustBuildDir/${variant.name}"))
                    it.args.set(extension.args)
                    it.env.set(extension.env)
                }

                // Link the variant-specific task output to the variant's jniLibs
                variant.sources.jniLibs?.addGeneratedSourceDirectory(
                    buildTask
                ) { it.installDir }
            }
        }
    }
}
