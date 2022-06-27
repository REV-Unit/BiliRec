plugins {
    application
    kotlin("jvm")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("plugin.spring")
}

dependencies {
    implementation(project(":network"))
    implementation(project(":recording"))
    implementation(project(":logging"))
    implementation(project(":flv"))
    implementation(project(":config"))
    implementation(project(":events"))
    implementation(project(":web"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation("commons-cli:commons-cli:1.5.0")
    implementation("org.springframework.boot:spring-boot-starter"){
        exclude(group="org.springframework.boot",module="spring-boot-starter-logging")
    }
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}