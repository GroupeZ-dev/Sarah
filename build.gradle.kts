import java.util.Locale

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2" // Pour remplacer maven-shade-plugin
    `maven-publish`
}

rootProject.extra.properties["sha"]?.let { sha ->
    version = sha
}

group = "fr.maxlego08.sarah"
version = "1.17"

extra.set("targetFolder", file("target/"))
extra.set("apiFolder", file("target-api/"))
extra.set("classifier", System.getProperty("archive.classifier"))
extra.set("sha", System.getProperty("github.sha"))

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

publishing {

    var repository = System.getProperty("repository.name", "snapshots").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    repositories {
        maven {
            name = "groupez${repository}"
            url = uri("https://repo.groupez.dev/${repository.lowercase()}")
            credentials {
                username = findProperty("${name}Username") as String? ?: System.getenv("MAVEN_USERNAME")
                password = findProperty("${name}Password") as String? ?: System.getenv("MAVEN_PASSWORD")
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }

    publications {
        register<MavenPublication>("groupez${repository}") {
            pom {
                groupId = project.group as String?
                name = "${rootProject.name}-${project.name}"
                artifactId = name.get().lowercase()
                version = project.version as String?

                scm {
                    connection = "scm:git:git://github.com/GroupeZ-dev/${rootProject.name}.git"
                    developerConnection = "scm:git:ssh://github.com/GroupeZ-dev/${rootProject.name}.git"
                    url = "https://github.com/GroupeZ-dev/${rootProject.name}/"
                }
            }
            artifact(tasks.shadowJar)
        }
    }
}