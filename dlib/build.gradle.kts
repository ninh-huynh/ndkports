import com.android.ndkports.CMakeCompatibleVersion
import com.android.ndkports.TarPortTask

val portVersion = "19.24.6"

group = "com.android.ndk.thirdparty"
version = "$portVersion${rootProject.extra.get("snapshotSuffix")}"

plugins {
    id("maven-publish")
    id("com.android.ndkports.NdkPorts")
    distribution
}

ndkPorts {
    ndkPath.set(File(project.findProperty("ndkPath") as String))
//    source.set(project.file("v19.24.6.tar.gz"))
    source.set(project.file("dlib-libs.tar.gz"))
    minSdkVersion.set(21)
}

//val buildTask = tasks.register<CMakePortTask>("buildPort") {
//    cmake {
//        arg("-DANDROID_CPP_FEATURES=rtti exceptions")
//    }
//}

val buildTask = tasks.register<TarPortTask>("buildPort") {

}


tasks.prefabPackage {
    version.set(CMakeCompatibleVersion.parse(portVersion))

    licensePath.set("")

    modules {
        create("dlib") {
            static.set(true)
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["prefab"])
            pom {
                name.set("Dlib")
                description.set("The ndkports AAR for Dlib.")
                url.set(
                    "https://android.googlesource.com/platform/tools/ndkports"
                )
                licenses {
                    license {
                        name.set("Boost Software License 1.0")
                        url.set("https://github.com/davisking/dlib/blob/master/LICENSE.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        name.set("The Android Open Source Project")
                    }
                }
            }
        }
    }

    repositories {
        maven {
            url = uri("${project.buildDir}/repository")
        }
    }
}

distributions {
    main {
        contents {
            from("${project.buildDir}/repository")
            include("**/*.aar")
            include("**/*.pom")
        }
    }
}

tasks {
    distZip {
        dependsOn("publish")
        destinationDirectory.set(File(rootProject.buildDir, "distributions"))
    }
}
