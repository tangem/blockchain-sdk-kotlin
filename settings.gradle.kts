pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)

    repositories {
        google()
        mavenCentral()
        mavenLocal()

        val properties = java.util.Properties()
        val localPropertiesFile = file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        listOf(
            "https://maven.pkg.github.com/tangem/blst-android",
            "https://maven.pkg.github.com/tangem/tangem-sdk-android",
            "https://maven.pkg.github.com/tangem/blockchain-sdk-kotlin",
        ).forEach { repoUrl ->
            maven {
                url = uri(repoUrl)
                credentials {
                    username = properties.getProperty("gpr.user", System.getenv("GITHUB_ACTOR"))
                    password = properties.getProperty("gpr.key", System.getenv("GITHUB_TOKEN"))
                }
            }
        }

        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "blockchain-sdk-kotlin"
include(":blockchain", ":blockchain-demo")