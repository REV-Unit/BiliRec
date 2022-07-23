repositories {
    mavenCentral()
}
tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

dependencies {
    implementation(project(":network"))
    implementation(project(":config"))
    implementation(project(":logging"))
    implementation(project(":flv"))
    implementation(project(":common"))
    implementation("org.glassfish.jaxb:txw2:4.0.0")
}