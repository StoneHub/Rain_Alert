import org.gradle.api.initialization.resolve.RepositoriesMode.*

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
//noinspection UnstableApiUsage
dependencyResolutionManagement {
    //noinspection UnstableApiUsage
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    //noinspection UnstableApiUsage
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Rain Alert"
include(":app")
 