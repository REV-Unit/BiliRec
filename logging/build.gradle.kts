plugins {
    kotlin("jvm")
    `java-library`
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
}
dependencies {
    implementation("org.springframework.boot:spring-boot-starter-log4j2")
}