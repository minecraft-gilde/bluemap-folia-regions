rootProject.name = "BlueMap-Folia-Regions"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            plugin("paper", "io.papermc.paperweight.userdev").version("2.0.0-beta.21")
            plugin("runpaper", "xyz.jpenilla.run-paper").version("2.3.0")
        }
    }
}
