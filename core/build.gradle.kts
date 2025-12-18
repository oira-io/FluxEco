plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
    id("de.eldoria.plugin-yml.bukkit") version "0.8.0"
}

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

    library("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", "2.2.21")
    library("org.jetbrains.kotlinx", "kotlinx-serialization-json", "1.9.0")

    val lampVersion = "4.0.0-rc.14"
    val exposedVersion = "0.61.0"

    library("io.github.revxrsal", "lamp.common", lampVersion)
    library("io.github.revxrsal", "lamp.bukkit", lampVersion)
    library("io.github.revxrsal", "lamp.brigadier", lampVersion)

    library("org.jetbrains.exposed", "exposed-core", exposedVersion)
    library("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    library("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    library("org.jetbrains.exposed", "exposed-java-time", exposedVersion)
    library("org.jetbrains.exposed", "exposed-kotlin-datetime", exposedVersion)

    library("com.zaxxer", "HikariCP", "7.0.2")

    library("org.xerial", "sqlite-jdbc", "3.51.0.0")
    library("com.mysql", "mysql-connector-j", "9.5.0")
    library("com.h2database", "h2", "2.4.240")

    library("org.mongodb", "mongodb-driver-kotlin-coroutine", "5.6.1")
    library("org.mongodb", "bson-kotlinx", "5.6.1")

    implementation("de.rapha149.signgui", "signgui", "2.5.4")
    implementation("com.tcoded", "FoliaLib", "0.5.1")
    library("redis.clients", "jedis", "7.0.0")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:3.1.0")
    compileOnly("io.github.miniplaceholders:miniplaceholders-kotlin-ext:3.1.0")
}

bukkit {
    name = "FluxEco"
    main = "io.oira.fluxeco.core.FluxEco"

    authors = listOf("Harfull")
    description = "A modern, optimized, and lightweight economy plugin."

    depend = listOf("Vault")
    softDepend = listOf("PlaceholderAPI", "MiniPlaceholders")

    apiVersion = "1.21"
    foliaSupported = true
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
