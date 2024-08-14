rootProject.name = "jpa-event-listener"

pluginManagement {
    val springBootVersion: String by settings // from gradle.properties
    val kotlinVersion: String by settings // from gradle.properties

    // 플러그인 버전은 여기에만 명시, 하위 프로젝트에서는 버전 명시 없이 사용!
    plugins {
        // Spring Boot for Kotlin
        id("io.spring.dependency-management") version "1.0.13.RELEASE"
        id("org.springframework.boot") version springBootVersion
        kotlin("jvm") version kotlinVersion
        kotlin("plugin.spring") version kotlinVersion
        // Etc
        kotlin("plugin.jpa") version kotlinVersion
        kotlin("kapt") version kotlinVersion
        kotlin("plugin.noarg") version kotlinVersion
    }
}
