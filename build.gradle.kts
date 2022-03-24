import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "7.1.0"
}

allprojects{
    group = "moe.peanutmelonseedbigalmond"
    version = "1.0-SNAPSHOT"
    repositories {
        maven("https://maven.aliyun.com/repository/central")
        maven("https://jitpack.io")
    }
}

dependencies {
    implementation(project(":app"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    implementation(kotlin("stdlib-jdk8"))
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "moe.peanutmelonseedbigalmond.bilirec.app.MainKt"
            )
        )
    }
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>{
    archiveBaseName.set("BiliRec")
}