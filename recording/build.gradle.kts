tasks.bootJar{
    enabled=false
}

tasks.jar{
    enabled=true
}

dependencies {
    implementation(project(":network"))
    implementation(project(":config"))
    implementation(project(":logging"))
    implementation(project(":flv"))
    implementation(project(":events"))
}