package com.android.ndkports

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class SymlinkSourceTask: DefaultTask() {

    @get:InputDirectory
    abstract val sourceDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outDir: DirectoryProperty

    @TaskAction
    fun run() {

        outDir.get().asFile.delete()

        val pb = ProcessBuilder(
            listOf(
                "ln",
                "-s",
                sourceDir.get().asFile.absolutePath,
                outDir.get().asFile.absolutePath,
            )
        ).redirectErrorStream(true)

        val result = pb.start()
        val output = result.inputStream.bufferedReader().use { reader ->
            var lines = ""
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                println(line)
                lines += line
            }

            lines
        }
        if (result.waitFor() != 0) {
            throw RuntimeException("Subprocess failed with:\n$output")
        }
    }

}