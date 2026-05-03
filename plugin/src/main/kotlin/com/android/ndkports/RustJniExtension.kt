package com.android.ndkports

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class RustJniExtension @Inject constructor(objects: ObjectFactory) {

    abstract val sourceDir: DirectoryProperty

    abstract val libName: Property<String>

    val buildType: Property<String> =
        objects.property(String::class.java).convention("release")

    val targetAbis: ListProperty<String> =
        objects.listProperty(String::class.java).convention(
            listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        )

    val args: ListProperty<String> =
        objects.listProperty(String::class.java).convention(emptyList())

    val env: MapProperty<String, String> =
        objects.mapProperty(String::class.java, String::class.java).convention(emptyMap())
}
