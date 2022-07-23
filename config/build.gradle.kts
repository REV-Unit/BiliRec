tasks.bootJar {
    enabled = false
}

tasks.jar {
    enabled = true
}

plugins{
    kotlin("plugin.serialization")
}

dependencies {
    implementation("com.charleskorn.kaml:kaml:0.46.0")
}
