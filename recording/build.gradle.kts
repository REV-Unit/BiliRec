plugins {
    `java-library`
    kotlin("jvm")
}

dependencies {
    implementation(project(":network"))
    implementation(project(":config"))
    implementation(project(":logging"))
    implementation(project(":flv"))
    implementation(project(":events"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
}