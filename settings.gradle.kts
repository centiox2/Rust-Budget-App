pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositories { google(); mavenCentral() } }
rootProject.name = "VerdantBudget"
include(":shared", ":androidApp", ":desktopApp", ":releaseDriver")
