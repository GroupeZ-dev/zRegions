// Module API publique : zéro dépendance plateforme (ni Bukkit, ni zMenu, ni Adventure).
// Publié plus tard vers target-api/ (re.alwyn974.groupez.publish) pour les addons tiers.

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
