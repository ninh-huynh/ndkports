rootProject.name = "ndkports"

pluginManagement {
    repositories {
        mavenCentral()
        google()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "kotlinx-serialization") {
                useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
            }
        }
    }

    includeBuild("./plugin") {
        name = "plugin-build-context"
    }
}

include("curl")
include("googletest")
include("jsoncpp")
include("openssl")
include("plugin")