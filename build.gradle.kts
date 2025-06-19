plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2" // Pour remplacer maven-shade-plugin
}

group = "fr.maxlego08.sarah"
version = "1.17"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.zaxxer:HikariCP:4.0.3")
}

tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Title"] = "Sarah"
        attributes["Implementation-Version"] = project.version
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("") // Écrase le JAR de base par le fat jar
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
