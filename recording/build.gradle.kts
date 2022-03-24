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
}