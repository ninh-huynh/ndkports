package com.android.ndkports

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

abstract class RustJniPortTask : PortTask() {

    @get:Input
    abstract val libName: Property<String>

    @get:Input
    abstract val buildType: Property<String>

    @get:Input
    abstract val env: MapProperty<String, String>

    @get:Input
    abstract val args: ListProperty<String>

    override fun buildForAbi(
        toolchain: Toolchain,
        workingDirectory: File,
        buildDirectory: File,
        installDirectory: File
    ) {
        val cargoTarget = if (toolchain.abi == Abi.Arm) {
            "armv7-linux-androideabi"
        } else {
            toolchain.binutilsTriple
        }

        // https://mozilla.github.io/firefox-browser-architecture/experiments/2017-09-21-rust-on-android.html
        val rustFlags = mutableListOf(
            "-C", "linker=${toolchain.clang.absolutePath}",
            "-C", "link-arg=-Wl,--build-id=sha1"
        )

        val soname = libName.get().let { "lib$it.so" }
        rustFlags.addAll(listOf("-C", "link-arg=-Wl,-soname,$soname"))
        rustFlags.addAll(listOf("-C", "link-arg=-Wl,-z,max-page-size=16384"))
        val cargoArgs = mutableListOf(
            "cargo",
            "build",
            "--target=$cargoTarget",
            "--target-dir",
            buildDirectory.absolutePath,
        )

        if (buildType.get() == "release") {
            cargoArgs.add("--release")
        }

        val requestedCargoArgs = args.get()
        if (requestedCargoArgs.isNotEmpty()) {
            cargoArgs.addAll(requestedCargoArgs)
        }
        val requestedEnv = env.get()

        val cargoEnv = mutableMapOf(
            "RUSTFLAGS" to rustFlags.joinToString(" "),
            "CARGO_PROFILE_RELEASE_STRIP" to "false",
            "CARGO_PROFILE_RELEASE_DEBUG" to "1",
        ).apply {
            if (toolchain.abi == Abi.Arm) {
                put("CARGO_PROFILE_RELEASE_PANIC", "abort")
            }
            if (requestedEnv.isNotEmpty()) {
                putAll(requestedEnv)
            }
        }

        println("Running cargo build for ${toolchain.abi}:")
        println("Command: ${cargoArgs.joinToString(" ")}")
        println("Environment: $cargoEnv")

        executeSubprocess(
            cargoArgs,
            sourceDirectory.get().asFile,
            additionalEnvironment = cargoEnv
        )

        val profileDir = if (buildType.get() == "release") "release" else "debug"

        val sourceDir = buildDirectory.resolve("$cargoTarget/$profileDir").toPath()
        installDirectory.mkdirs()

        Files.newDirectoryStream(sourceDir, soname).use { stream ->
            for (path in stream) {
                if (Files.isRegularFile(path)) {
                    val targetPath = installDirectory.resolve(soname).toPath()
                    Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    override fun buildDirectoryFor(abi: Abi): File =
        buildDir.asFile.get().resolve("build/${abi.abiName}")

    override fun installDirectoryFor(abi: Abi): File =
        installDir.get().asFile.resolve(abi.abiName)
}
