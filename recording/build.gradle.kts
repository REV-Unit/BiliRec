plugins {
    `java-library`
    kotlin("jvm")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":network"))
    implementation(project(":config"))
    implementation(project(":logging"))
    implementation(project(":flv"))
    implementation(project(":events"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
}