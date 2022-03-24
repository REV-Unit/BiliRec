plugins {
    application
    kotlin("jvm")
}

dependencies {
    implementation(project(":network"))
    implementation(project(":recording"))
    implementation(project(":logging"))
    implementation(project(":flv"))
    implementation(project(":config"))
    implementation(project(":events"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation("commons-cli:commons-cli:1.5.0")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}