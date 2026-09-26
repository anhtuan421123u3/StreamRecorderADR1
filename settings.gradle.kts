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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "LiveVault"

// ── App ────────────────────────────────────────────────────
include(":app")

// ── Core ───────────────────────────────────────────────────
include(":core:common")
include(":core:network")
include(":core:database")
include(":core:ui")

// ── Features ───────────────────────────────────────────────
include(":feature:auth")
include(":feature:home")
include(":feature:channel")
include(":feature:library")
include(":feature:player")
include(":feature:notifications")
include(":feature:settings")
include(":feature:paywall")
