plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    implementation ("cn.hutool:hutool-core:5.7.21")
    implementation(fileTree("../lib"))
    implementation(project(":logging"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
}