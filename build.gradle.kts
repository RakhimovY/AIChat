plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.2.4"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "AIChat"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val springAiVersion = "1.0.0-SNAPSHOT"

repositories {
    repositories {
        mavenCentral()
        maven { url = uri("https://repo.spring.io/milestone") }
        maven { url = uri("https://repo.spring.io/snapshot") }
        maven {
            name = "Central Portal Snapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
        }
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:$springAiVersion")
    }
}

dependencies {
    // Spring AI
    implementation(platform("org.springframework.ai:spring-ai-bom:$springAiVersion"))
    implementation("org.springframework.ai:spring-ai-openai-spring-boot-starter:$springAiVersion")
    implementation("org.springframework.ai:spring-ai-starter-model-chat-memory-jdbc:$springAiVersion")
    implementation("org.springframework.ai:spring-ai-openai")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    // Database
    runtimeOnly("org.postgresql:postgresql")

    // Dev & Test
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
