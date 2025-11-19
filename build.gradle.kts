import java.util.Locale

plugins {
    `java-library`
    id("com.github.johnrengelman.shadow") version "7.1.2" // Pour remplacer maven-shade-plugin
    `maven-publish`
}

rootProject.extra.properties["sha"]?.let { sha ->
    version = sha
}

group = "fr.maxlego08.sarah"
version = "1.20.2"


extra.set("targetFolder", file("target/"))
extra.set("apiFolder", file("target-api/"))
extra.set("classifier", System.getProperty("archive.classifier"))
extra.set("sha", System.getProperty("github.sha"))

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8

    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.zaxxer:HikariCP:4.0.3")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")
    testImplementation("org.xerial:sqlite-jdbc:3.42.0.0")
    testImplementation("org.mariadb.jdbc:mariadb-java-client:3.1.4")
    testImplementation("com.mysql:mysql-connector-j:8.0.33")
}

tasks.withType<Jar> {
    manifest {
        attributes["Implementation-Title"] = "Sarah"
        attributes["Implementation-Version"] = project.version
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveClassifier.set("") // Écrase le JAR de base par le fat jar

    relocate("com.zaxxer.hikari", "fr.maxlego08.sarah.libs.hikari")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.test {
    useJUnitPlatform()
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
                artifactId = rootProject.name.lowercase()
                version = if (repository.lowercase() == "snapshots") {
                    System.getProperty("github.sha")
                } else {
                    project.version as String?
                }

                scm {
                    connection = "scm:git:git://github.com/GroupeZ-dev/${rootProject.name}.git"
                    developerConnection = "scm:git:ssh://github.com/GroupeZ-dev/${rootProject.name}.git"
                    url = "https://github.com/GroupeZ-dev/${rootProject.name}/"
                }
            }
            artifact(tasks.shadowJar)
            // artifact(tasks.javadocJar)
            // artifact(tasks.sourcesJar)
        }
    }
}