// Hook zMenu : GUI optionnel (liste des régions, menu par région, éditeur de flags).
// Règles écosystème : l'API cible est TOUJOURS compileOnly (jamais shadée, jamais liée
// tant que ZRegionsBukkitPlugin n'a pas vérifié la présence du plugin zMenu), et le hook
// ne dépend jamais du module :bukkit (il est shadé PAR lui — un project dep serait un cycle) :
// le loader JavaPlugin et la factory joueur arrivent par le constructeur.

dependencies {
    compileOnly(project(":common"))
    compileOnly("org.spigotmc:spigot-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("fr.maxlego08.menu:zmenu-api:1.1.1.6")
}
