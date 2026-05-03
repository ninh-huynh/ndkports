import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val kotlinVersion = "2.1.20"

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("java-gradle-plugin")
    id("maven-publish")
}

group = "com.android.ndkports"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation(kotlin("stdlib", kotlinVersion))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.3")

    implementation("com.google.prefab:api:1.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("org.redundent:kotlin-xml-builder:1.6.1")

    testImplementation(kotlin("test", kotlinVersion))
    testImplementation(kotlin("test-junit", kotlinVersion))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
}

tasks {
    compileJava {
        options.release.set(11)
    }

    compileKotlin {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
    }

    compileTestKotlin {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
    }
}

gradlePlugin {
    plugins {
        create("ndkports") {
            id = "com.android.ndkports.NdkPorts"
            implementationClass = "com.android.ndkports.NdkPortsPlugin"
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri("${rootProject.buildDir}/repository")
        }
    }
}
