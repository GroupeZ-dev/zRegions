// Module API publique : zéro dépendance plateforme (ni Bukkit, ni zMenu, ni Adventure).
// Publié vers target-api/ et repo.groupez.dev (re.alwyn974.groupez.publish) pour les addons tiers.

plugins {
    id("com.gradleup.shadow") version "9.0.0"
    id("re.alwyn974.groupez.publish") version "1.0.0"
}

// En CI, chaque build publie un artefact versionné par le SHA du commit.
rootProject.extra.properties["sha"]?.let { sha ->
    version = sha
}

java {
    withSourcesJar()
    withJavadocJar()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    shadowJar {
        destinationDirectory.set(rootProject.extra["apiFolder"] as File)
    }

    build {
        dependsOn(shadowJar)
    }

    javadoc {
        options.encoding = "UTF-8"
        if (JavaVersion.current().isJava9Compatible) {
            (options as StandardJavadocDocletOptions).addBooleanOption("html5", true)
        }
    }
}

publishConfig {
    githubOwner.set("GroupeZ-dev")
}
