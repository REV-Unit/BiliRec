tasks.bootJar{
	enabled=false
}

tasks.jar{
	enabled=true
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web"){
		exclude(group="org.springframework.boot",module="spring-boot-starter-logging")
	}
}
