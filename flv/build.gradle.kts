plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    implementation ("cn.hutool:hutool-core:5.7.21")
    implementation(fileTree("../lib"))
    implementation(project(":logging"))
}