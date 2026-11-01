pluginManagement {
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories { mavenCentral() }
}

rootProject.name = "lilserve"

include("kernel")

include("reservation-service:domain")

include("reservation-service:application")

include("reservation-service:persistence")

include("reservation-service:web")

include("reservation-service:launcher")
