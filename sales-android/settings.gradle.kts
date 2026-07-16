pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "9.3.0"
        id("com.android.library") version "9.3.0"
        id("org.jetbrains.kotlin.plugin.parcelize") version "2.4.10"
        id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "PharmaSmartSimpleSale"
