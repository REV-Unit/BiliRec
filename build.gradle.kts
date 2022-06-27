import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.6.20"
    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("org.springframework.boot") version "3.0.0-M3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("plugin.spring") version "1.6.21"
}

allprojects {
    group = "moe.peanutmelonseedbigalmond"
    version = "1.0-SNAPSHOT"
    repositories {
//        mavenCentral()
        maven("https://maven.aliyun.com/repository/central")
        maven("https://jitpack.io")
        maven("https://repo.spring.io/milestone")
//        maven("https://maven.aliyun.com/repository/spring")
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

java.sourceCompatibility=JavaVersion.VERSION_17

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
    kotlinOptions.freeCompilerArgs=listOf("-Xjsr305=strict")
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar>{
    enabled=false
}

tasks.withType<Jar> {
    manifest {
        attributes(
            mapOf(
                "Main-Class" to "moe.peanutmelonseedbigalmond.bilirec.BiliRecApplicationKt"
            )
        )
    }
}



tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    archiveBaseName.set("BiliRec")
}