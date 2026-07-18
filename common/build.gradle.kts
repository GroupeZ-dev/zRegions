// Module common : TOUTE la logique, platform-agnostic.
// Règle absolue : aucun import org.bukkit.*, zMenu ou API serveur ici (cf. ARCHITECTURE.md §1).

dependencies {
    api(project(":api"))

    // ORM maison (shadée + relocatée par le module plateforme)
    implementation("fr.maxlego08.sarah:sarah:1.23")

    // Texte cross-plateforme (Component + MiniMessage) — l'envoi est délégué à la plateforme
    api("net.kyori:adventure-api:4.17.0")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")

    // Sérialisation des messages multi-serveur & shape_data
    implementation("com.google.code.gson:gson:2.11.0")

    // Lecture des fichiers d'autres plugins (importateur WorldGuard) — shadé + relocaté,
    // car common doit rester portable (Fabric/Nukkit n'embarquent pas snakeyaml)
    implementation("org.yaml:snakeyaml:2.2")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.46.0.0")
}
