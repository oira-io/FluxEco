plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow") version "9.2.2"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
}

group = "io.oira"

repositories {
    maven("https://repo.flyte.gg/releases")
    maven("https://repo.tcoded.com/releases")
    maven("https://maven.enginehub.org/repo/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    implementation(project(":api"))

    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")

    compileOnly("io.github.revxrsal:lamp.common:4.0.0-rc.13")
    compileOnly("io.github.revxrsal:lamp.bukkit:4.0.0-rc.13")
    compileOnly("io.github.revxrsal:lamp.brigadier:4.0.0-rc.13")

    compileOnly("com.zaxxer:HikariCP:7.0.2")
    compileOnly("org.jetbrains.exposed:exposed-core:0.61.0")
    compileOnly("org.jetbrains.exposed:exposed-dao:0.61.0")
    compileOnly("org.jetbrains.exposed:exposed-jdbc:0.61.0")
    compileOnly("org.jetbrains.exposed:exposed-java-time:0.61.0")
    compileOnly("org.jetbrains.exposed:exposed-kotlin-datetime:0.61.0")

    compileOnly("org.xerial:sqlite-jdbc:3.50.3.0")
    compileOnly("com.mysql:mysql-connector-j:9.4.0")
    compileOnly("com.h2database:h2:2.4.240")

    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:3.0.1")
    compileOnly("io.github.miniplaceholders:miniplaceholders-kotlin-ext:3.0.1")

    implementation("de.rapha149.signgui:signgui:2.5.4")
    implementation("com.tcoded:FoliaLib:0.5.1")
}

val targetJavaVersion = 21

kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.set(listOf("-Xannotation-default-target=param-property"))
    }
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveBaseName.set("FluxEco")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    destinationDirectory.set(file("$rootDir/dist"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    mergeServiceFiles()

    relocate("com.tcoded.folialib", "io.oira.fluxeco.lib.folialib")
    relocate("de.rapha149.signgui", "io.oira.fluxeco.lib.signgui")

    exclude("kotlin/**", "kotlinx/**", "org/jetbrains/**", "org/intellij/**", "META-INF/kotlin*", "META-INF/*.kotlin_module")

    from(project(":api").sourceSets["main"].output)
}

tasks.clean {
    dependsOn(":cleanDist")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.runServer {
    minecraftVersion("1.21")
}
