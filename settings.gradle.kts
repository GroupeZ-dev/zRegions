pluginManagement {
    repositories {
        maven {
            name = "groupezReleases"
            url = uri("https://repo.groupez.dev/releases")
        }
        gradlePluginPortal()
    }
}

rootProject.name = "zRegions"

include("api", "common", "bukkit")

// Hooks auto-inclus (modèle zMenu/zKoth) : chaque sous-dossier de Hooks/ avec un build devient un module :Hooks:<Nom>
java.io.File(rootDir, "Hooks").listFiles()
    ?.filter { it.isDirectory && it.name != "build" && java.io.File(it, "build.gradle.kts").exists() }
    ?.forEach { include(":Hooks:${it.name}") }
