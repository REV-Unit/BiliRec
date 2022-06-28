import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.20"
//    id("com.github.johnrengelman.shadow") version "7.1.0"
    id("org.springframework.boot") version "3.0.0-M3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("plugin.spring") version "1.6.21"
}

group = "moe.peanutmelonseedbigalmond"
version = "1.0-SNAPSHOT"
java.sourceCompatibility=JavaVersion.VERSION_17

allprojects{
    repositories {
//        mavenCentral()
        maven("https://maven.aliyun.com/repository/central")
        maven("https://jitpack.io")
        maven("https://repo.spring.io/milestone")
//        maven("https://maven.aliyun.com/repository/spring")
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar>{
    setProperty("mainClass","moe.peanutmelonseedbigalmond.bilirec.BiliRecApplicationKt")
}

//tasks.withType<Jar> {
//    manifest {
//        attributes(
//            mapOf(
//                "Main-Class" to "moe.peanutmelonseedbigalmond.bilirec.BiliRecApplicationKt"
//            )
//        )
//    }
//}
//
//tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
//    archiveBaseName.set("BiliRec")
//}

subprojects{
    apply {
        plugin("org.jetbrains.kotlin.jvm")
//        plugin("com.github.johnrengelman.shadow")
        plugin("org.springframework.boot")
        plugin("io.spring.dependency-management")
        plugin("org.jetbrains.kotlin.plugin.spring")
    }

    dependencies {
//        implementation(project(":app"))
        implementation(kotlin("stdlib-jdk8"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
        implementation("commons-cli:commons-cli:1.5.0")
        implementation("org.springframework.boot:spring-boot-starter"){
            exclude(group="org.springframework.boot",module="spring-boot-starter-logging")
        }
        developmentOnly("org.springframework.boot:spring-boot-devtools")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        implementation("org.greenrobot:eventbus-java:3.3.1")
        testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
        testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.freeCompilerArgs=listOf("-Xjsr305=strict")
    }

//    tasks.getByName<Test>("test") {
//        useJUnitPlatform()
//    }
}