// Hook PlaceholderAPI : expansion sortante %zregions_…%. Comme le hook zMenu,
// l'API cible est TOUJOURS compileOnly (jamais shadée, jamais liée tant que
// ZRegionsBukkitPlugin n'a pas vérifié la présence de PlaceholderAPI), et le hook
// ne dépend jamais du module :bukkit (il est shadé PAR lui — ce serait un cycle) :
// le plugin arrive par le constructeur.

dependencies {
    compileOnly(project(":common"))
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
}
