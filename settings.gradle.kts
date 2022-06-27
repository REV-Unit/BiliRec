include("network")
include("app")
rootProject.name = "BiliRec"
include("recording")
include("logging")
include("config")
include("flv")
include("events")
include("web")

pluginManagement {
    repositories {
        maven("https://repo.spring.io/milestone")
//        maven("https://maven.aliyun.com/repository/spring-plugin")
//        gradlePluginPortal()
        maven("https://maven.aliyun.com/repository/gradle-plugin")
    }
}
