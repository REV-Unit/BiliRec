tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

dependencies {
    implementation(fileTree("../lib"))
    implementation(project(":logging"))
    implementation("org.glassfish.jaxb:txw2:4.0.0")
    implementation(project(":common"))
}