pluginManagement {
    includeBuild("build-logic")
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

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "chronoflow-android"

include(":app")
include(":benchmark")
include(":konsist")
include(
    ":core:model",
    ":core:plan",
    ":core:sync",
    ":core:reminders",
    ":core:wizard",
    ":core:network",
    ":core:data-mock",
    ":core:testing",
    ":core:testing-junit",
    ":core:database",
    ":core:datastore",
    ":core:data",
    ":core:designsystem",
    ":core:navigation",
    ":core:ui",
)
include(
    ":feature:launch",
    ":feature:auth",
    ":feature:onboarding",
    ":feature:today",
    ":feature:trips",
    ":feature:trip",
    ":feature:wizard",
    ":feature:settings",
    ":feature:reminders",
)
include(":tools:tokengen")
