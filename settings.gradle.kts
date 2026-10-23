pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google() // Compose Desktop resolves some AndroidX JVM artifacts here.
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "MaviMusic"
include(":shared", ":desktopUi", ":desktopApp")
