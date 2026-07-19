plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.0.0" apply false
    id("re.alwyn974.groupez.repository") version "1.0.0"
}

version = "1.0.0"

// Publishing metadata: the API module's jar is published to repo.groupez.dev (and target-api/)
// for third-party addons. The CI passes -Darchive.classifier / -Dgithub.sha.
extra.set("targetFolder", file("target/"))
extra.set("apiFolder", file("target-api/"))
extra.set("classifier", System.getProperty("archive.classifier"))
extra.set("sha", System.getProperty("github.sha"))

allprojects {
    apply(plugin = "java-library")
    apply(plugin = "re.alwyn974.groupez.repository")

    group = "fr.maxlego08.zregions"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.groupez.dev/releases")
        maven("https://jitpack.io")
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
