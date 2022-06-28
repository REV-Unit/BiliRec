tasks.bootJar{
    enabled=false
}

tasks.jar{
    enabled=true
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation(fileTree("../lib"))
    api("com.squareup.okhttp3:okhttp:5.0.0-alpha.4")
    api("org.json:json:20211205")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.brotli:dec:0.1.2")
    implementation("com.github.haroldadmin:NetworkResponseAdapter:4.2.2")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(project(":events"))
}