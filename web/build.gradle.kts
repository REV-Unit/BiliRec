plugins {
	`java-library`
	kotlin("jvm")
	id("org.springframework.boot")
	id("io.spring.dependency-management")
	kotlin("plugin.spring")
}

tasks.withType<org.springframework.boot.gradle.tasks.bundling.BootJar>{
	enabled=false
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
}
