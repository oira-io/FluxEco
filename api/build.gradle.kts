plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.dokka")
}

group = "io.oira"

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.jar {
    archiveBaseName.set("FluxEco-API")
    archiveVersion.set(project.version.toString())
    destinationDirectory.set(file("$rootDir/dist"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}
