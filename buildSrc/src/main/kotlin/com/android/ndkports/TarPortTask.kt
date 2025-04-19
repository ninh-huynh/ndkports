package com.android.ndkports

import java.io.File

abstract class TarPortTask: PortTask() {

    override fun buildForAbi(
        toolchain: Toolchain,
        workingDirectory: File,
        buildDirectory: File,
        installDirectory: File
    ) {
        sourceDirectory.get().asFile.resolve(toolchain.abi.abiName)
            .copyRecursively(installDirectory)
    }
}