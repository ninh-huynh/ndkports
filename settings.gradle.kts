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
}

include("curl")
include("googletest")
include("jsoncpp")
include("openssl")
include("plugin")