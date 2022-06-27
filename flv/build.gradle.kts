plugins {
    `java-library`
    kotlin("jvm")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
}

dependencies {
    implementation("cn.hutool:hutool-core:5.7.21")
    implementation(fileTree("../lib"))
    implementation(project(":logging"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
}