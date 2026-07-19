// Module bukkit : le pont jeu↔plugin. Produit le jar final target/zRegions.jar.
// Deux sourceSets (ARCHITECTURE.md §3) :
//  - main  → compile contre spigot-api  : compat Spigot GARANTIE par le compilateur (plugin vendu sur SpigotMC)
//  - paper → compile contre paper-api   : classes Paper/Folia isolées, instanciées uniquement après Class.forName

plugins {
    id("com.gradleup.shadow") version "9.0.0"
}

val paper: SourceSet by sourceSets.creating

dependencies {
    implementation(project(":common"))

    // sourceSet main : SPIGOT uniquement — tout appel Paper-only est refusé à la compilation.
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    // Envoi Adventure identique sur Spigot ET Paper (jamais l'Adventure natif Paper).
    implementation("net.kyori:adventure-platform-bukkit:4.3.4")
    // Télémétrie bStats (shadée + relocalisée pour éviter tout conflit inter-plugins).
    implementation("org.bstats:bstats-bukkit:3.1.0")

    // sourceSet paper : Paper/Folia (schedulers régionisés, AsyncTabCompleteEvent).
    "paperCompileOnly"("io.papermc.paper:paper-api:1.21.7-R0.1-SNAPSHOT")
    "paperImplementation"(sourceSets.main.get().output)
    "paperImplementation"(project(":common"))

    // Hooks auto-inclus
    rootProject.subprojects
        .filter { it.path.startsWith(":Hooks:") }
        .forEach { implementation(project(it.path)) }
}

tasks {
    jar { enabled = false }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    shadowJar {
        archiveBaseName.set("zRegions")
        archiveClassifier.set("")
        destinationDirectory.set(rootProject.file("target"))
        from(paper.output)

        relocate("fr.maxlego08.sarah", "fr.maxlego08.zregions.libs.sarah")
        relocate("net.kyori", "fr.maxlego08.zregions.libs.kyori")
        relocate("com.google.gson", "fr.maxlego08.zregions.libs.gson")
        // Spigot embarque son propre snakeyaml — la relocation évite tout conflit de version
        relocate("org.yaml.snakeyaml", "fr.maxlego08.zregions.libs.snakeyaml")
        // bStats impose la relocation (sinon conflit si un autre plugin l'embarque non relocalisé)
        relocate("org.bstats", "fr.maxlego08.zregions.libs.bstats")
    }

    build { dependsOn(shadowJar) }
}
