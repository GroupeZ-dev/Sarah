import java.util.Locale

plugins {
    `java-library`
    id("re.alwyn974.groupez.publish") version "1.0.0"
    id("com.gradleup.shadow") version "9.0.0-beta11"
    `maven-publish`
}

extra.set("targetFolder", file("target/"))
extra.set("classifier", System.getProperty("archive.classifier"))
extra.set("sha", System.getProperty("github.sha"))

group = "fr.maxlego08.sarah"
version = "1.21.2"

rootProject.extra.properties["sha"]?.let { sha ->
    version = sha
}

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

    // SQL Connectors - Available transitively for library users
    api("org.xerial:sqlite-jdbc:3.42.0.0")
    api("org.mariadb.jdbc:mariadb-java-client:3.1.4")
    api("com.mysql:mysql-connector-j:8.2.0")

    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")
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

    // Exclude SQL connectors from shadow jar - keep them as transitive dependencies
    dependencies {
        exclude(dependency("org.xerial:sqlite-jdbc"))
        exclude(dependency("org.mariadb.jdbc:mariadb-java-client"))
        exclude(dependency("com.mysql:mysql-connector-j"))
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.shadowJar {
    archiveClassifier.set("")
    destinationDirectory.set(rootProject.extra["targetFolder"] as File)
}

tasks.test {
    useJUnitPlatform()
}

publishConfig {
    githubOwner = "GroupeZ-dev"
    useRootProjectName = true
}