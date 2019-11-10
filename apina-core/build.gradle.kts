plugins {
    kotlin("jvm")
    java
    `maven-publish`
}

val kotlinVersion: String by rootProject.extra

dependencies {
    // We have to define explicit version here or invalid POM is generated
    compile(kotlin("stdlib", kotlinVersion))
    compile("org.slf4j:slf4j-api:1.7.12")
    compile("org.ow2.asm:asm:7.0")

    testImplementation(kotlin("test"))
    testImplementation("com.fasterxml.jackson.core:jackson-annotations:2.8.6")
    testImplementation("org.springframework:spring-web:4.3.5.RELEASE")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

val sourcesJar = task<Jar>("sourcesJar") {
    dependsOn("classes")
    archiveClassifier.set("sources")

    from(sourceSets.main.get().allSource)
}

val javadoc: Javadoc by tasks

val javadocJar = task<Jar>("javadocJar") {
    dependsOn(javadoc)
    archiveClassifier.set("javadoc")
    from(javadoc.destinationDir)
}

artifacts.add("archives", sourcesJar)
artifacts.add("archives", javadocJar)

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            artifact(javadocJar)
        }
    }
    if (hasProperty("mavenUser")) {
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
}



