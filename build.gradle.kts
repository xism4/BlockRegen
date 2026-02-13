plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.allync.blockregen"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.nightexpressdev.com/releases")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")

    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://maven.devs.beer/")
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("dev.lone:api-itemsadder:4.0.10")

    compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT")
    compileOnly("net.Indyuce:MMOItems-API:6.9.5-SNAPSHOT")
    compileOnly("net.Indyuce:MMOCore-API:1.12.1-SNAPSHOT")

    implementation("com.github.MilkBowl:VaultAPI:1.7.1")
    implementation("su.nightexpress.coinsengine:CoinsEngine:2.5.3")
}

tasks {
    processResources {
        filteringCharset = "UTF-8"
        filesMatching("**/*.yml") {
            expand(project.properties)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        mergeServiceFiles()
    }

    build {
        dependsOn(shadowJar)
    }
}
