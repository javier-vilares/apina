plugins {
    kotlin("jvm")
    id("com.gradle.plugin-publish") version "0.10.0"
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    compile(project(":apina-core"))

    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

pluginBundle {
    website = "https://github.com/javier-vilares/apina"
    vcsUrl = "https://github.com/javier-vilares/apina"
    description = "Gradle plugin for creating TypeScript client code from Spring controllers and Jackson classes"
    tags = listOf("typescript", "angular", "jackson", "spring")
}

gradlePlugin {
    plugins {
        create("apinaPlugin") {
            id = "es.enxenio.apina"
            displayName = "Gradle Apina plugin"
            implementationClass = "fi.evident.apina.gradle.ApinaPlugin"
        }
    }
}

publishing {

        repositories {
            maven {
                url =
                    uri(if (version.toString().endsWith("SNAPSHOT")) "http://nexus.ci.enxenio.net/repository/maven-snapshots/" else "http://nexus.ci.enxenio.net/repository/maven-releases")
                credentials {
                    username = property("mavenUser") as String
                    password = property("mavenPassword") as String
                }
            }
        }

}
