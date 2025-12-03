import net.minecrell.pluginyml.paper.PaperPluginDescription
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
    id("de.eldoria.plugin-yml.paper") version "0.8.0"
}

group = "io.oira"
version = "1.0"
val targetJavaVersion = 21

kotlin {
    jvmToolchain(targetJavaVersion)
}

repositories {
    mavenCentral()
    maven("https://repo.flyte.gg/releases")
    maven("https://repo.tcoded.com/releases")
    maven("https://maven.enginehub.org/repo/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {

    implementation(project(":api"))

    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.21-R0.1-SNAPSHOT")

    // Kotlin
    paperLibrary("org.jetbrains.kotlin", "kotlin-stdlib-jdk8")
    paperLibrary("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.6.0")

    // Lamp
    paperLibrary("io.github.revxrsal", "lamp.common", "4.0.0-rc.13")
    paperLibrary("io.github.revxrsal", "lamp.bukkit", "4.0.0-rc.13")
    paperLibrary("io.github.revxrsal", "lamp.brigadier", "4.0.0-rc.13")

    // Database
    paperLibrary("com.zaxxer", "HikariCP", "7.0.2")
    paperLibrary("org.jetbrains.exposed", "exposed-core", "0.61.0")
    paperLibrary("org.jetbrains.exposed", "exposed-dao", "0.61.0")
    paperLibrary("org.jetbrains.exposed", "exposed-jdbc", "0.61.0")
    paperLibrary("org.jetbrains.exposed", "exposed-java-time", "0.61.0")
    paperLibrary("org.jetbrains.exposed", "exposed-kotlin-datetime", "0.61.0")

    paperLibrary("org.xerial", "sqlite-jdbc", "3.50.3.0")
    paperLibrary("com.mysql", "mysql-connector-j", "9.4.0")
    paperLibrary("com.h2database", "h2", "2.4.240")

    // Others
    paperLibrary("de.rapha149.signgui", "signgui", "2.5.4")
    paperLibrary("com.tcoded", "FoliaLib", "0.5.1")
    paperLibrary("redis.clients", "jedis", "5.1.0")

    // Not embedded
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:3.0.1")
    compileOnly("io.github.miniplaceholders:miniplaceholders-kotlin-ext:3.0.1")
}

paper {
    name = "FluxEco"
    version = project.version.toString()
    main = "io.oira.fluxeco.FluxEco"
    apiVersion = "1.21"
    foliaSupported = true

    // Dependencies
    serverDependencies {
        register("Vault") {
            required = true
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("PlaceholderAPI") {
            required = false
        }
        register("MiniPlaceholders") {
            required = false
        }
    }

    generateLibrariesJson = true
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions.freeCompilerArgs.set(
        listOf("-Xannotation-default-target=param-property")
    )
}

tasks.jar { enabled = false }

tasks.shadowJar {
    archiveBaseName.set("FluxEco")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    destinationDirectory.set(file("$rootDir/dist"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    mergeServiceFiles()

    relocate("com.tcoded.folialib", "io.oira.fluxeco.lib.folialib")
    relocate("de.rapha149.signgui", "io.oira.fluxeco.lib.signgui")

    exclude(
        "kotlin/**",
        "kotlinx/**",
        "org/jetbrains/**",
        "org/intellij/**",
        "META-INF/kotlin*",
        "META-INF/*.kotlin_module"
    )

    from(project(":api").sourceSets["main"].output)
}

tasks.clean {
    dependsOn(":cleanDist")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}