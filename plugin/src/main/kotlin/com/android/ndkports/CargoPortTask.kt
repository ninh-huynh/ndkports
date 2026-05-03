package com.android.ndkports

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

class CargoBuilder(val toolchain: Toolchain, val sysroot: File): RunBuilder() {
    var isStatic = true
    var libName: String? = null
}

class CBindGenBuilder(val toolchain: Toolchain, val sysroot: File): RunBuilder() {
    var headerName = "bindings.h"
}

abstract class CargoPortTask : PortTask() {

    @get:Input
    abstract val cargo: Property<CargoBuilder.() -> Unit>

    @get:Optional
    @get:Input
    abstract val cbindgen: Property<CBindGenBuilder.() -> Unit>

    fun cargo(block: CargoBuilder.() -> Unit) = cargo.set(block)

    fun cbindgen(block: CBindGenBuilder.() -> Unit) = cbindgen.set(block)

    override fun buildForAbi(
        toolchain: Toolchain,
        workingDirectory: File,
        buildDirectory: File,
        installDirectory: File
    ) {
        // this is global command, could call it in any-place
        val cargoTarget = if (toolchain.abi == Abi.Arm) {
            "armv7-linux-androideabi"
        } else {
            toolchain.binutilsTriple
        }

        executeSubprocess(
            listOf(
                "rustup", "target", "add", cargoTarget
            ), workingDirectory
        )

        val cargoBlock = cargo.get()
        val cargoBuilder = CargoBuilder(
            toolchain,
            prefabGenerated.get().asFile.resolve(toolchain.abi.triple)
        )
        cargoBuilder.cargoBlock()

        val cbindgenBlock = cbindgen.orNull
        val cBindGenBuilder: CBindGenBuilder? = if (cbindgenBlock != null) {
            val builder = CBindGenBuilder(
                toolchain,
                prefabGenerated.get().asFile.resolve(toolchain.abi.triple)
            )
            builder.cbindgenBlock()
            builder
        } else {
            null
        }

        // https://mozilla.github.io/firefox-browser-architecture/experiments/2017-09-21-rust-on-android.html
        val rustFlags = mutableListOf(
            "-C", "linker=${toolchain.clang.absolutePath}",
            "-C", "link-arg=-Wl,--build-id=sha1"
        )

        if (!cargoBuilder.isStatic) {
            val soname = cargoBuilder.libName?.let { "lib$it.so" } ?: "lib${project.name}.so"
            rustFlags.addAll(listOf("-C", "link-arg=-Wl,-soname,$soname"))
            rustFlags.addAll(listOf("-C", "link-arg=-Wl,-z,max-page-size=16384"))
        }

        val cargoArgs = listOf(
            "cargo",
            "build",
            "--target=$cargoTarget",
            "--release",
            "--target-dir",
            buildDirectory.absolutePath,
        ) + cargoBuilder.cmd

        val cargoEnv = mutableMapOf(
            "RUSTFLAGS" to rustFlags.joinToString(" "),
            "CARGO_PROFILE_RELEASE_STRIP" to "false",
            "CARGO_PROFILE_RELEASE_DEBUG" to "1",
        ).apply {
            if (toolchain.abi == Abi.Arm) {
                put("CARGO_PROFILE_RELEASE_PANIC", "abort")
            }
            putAll(cargoBuilder.env)
        }

        println("Running cargo build for ${toolchain.abi}:")
        println("Command: ${cargoArgs.joinToString(" ")}")
        println("Environment: $cargoEnv")

        executeSubprocess(
            cargoArgs,
            sourceDirectory.get().asFile,
            additionalEnvironment = cargoEnv
        )

        if (cBindGenBuilder != null) {
            val cbindgenArgs = listOf(
                "cbindgen",
                "--output",
                "${installDirectory.absolutePath}/include/${cBindGenBuilder.headerName}"
            ) + cBindGenBuilder.cmd

            println("Running cbindgen for ${toolchain.abi}:")
            println("Command: ${cbindgenArgs.joinToString(" ")}")
            println("Environment: ${cBindGenBuilder.env}")

            executeSubprocess(
                cbindgenArgs,
                sourceDirectory.get().asFile,
                additionalEnvironment = cBindGenBuilder.env
            )
        }

        val sourceDir = buildDirectory.resolve("$cargoTarget/release").toPath()
        val targetLibDir = installDirectory.resolve("lib").toPath()
        val extension = if (cargoBuilder.isStatic) "a" else "so"
        val globPattern = cargoBuilder.libName?.let { "lib$it.$extension" } ?: "*.$extension"

        Files.createDirectories(targetLibDir)

        Files.newDirectoryStream(sourceDir, globPattern).use { stream ->
            for (path in stream) {
                if (Files.isRegularFile(path)) {
                    val targetPath = targetLibDir.resolve(path.fileName)
                    Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }
}
