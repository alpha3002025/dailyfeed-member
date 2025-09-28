plugins {
	java
	id("org.springframework.boot") version "3.5.5"
	id("io.spring.dependency-management") version "1.1.7"
}

subprojects {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")
}

allprojects {
    group = "click.dailyfeed"
    version = "0.0.1-SNAPSHOT"
    description = "Demo project for Spring Boot"

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    configurations {
        compileOnly {
            extendsFrom(configurations.annotationProcessor.get())
        }
    }

    repositories {
        mavenCentral()
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
        }
    }

    val querydslVersion = "5.0.0:jakarta"
    val mapstructVersion = "1.5.4.Final"

    dependencies {
        implementation(project(":dailyfeed-code"))
        implementation(project(":dailyfeed-feign"))
        implementation(project(":dailyfeed-pagination-support"))
        implementation(project(":dailyfeed-redis-support"))

        // spring
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        implementation("org.springframework.boot:spring-boot-starter-data-redis")
        implementation("org.springframework.boot:spring-boot-starter-validation")
        implementation("org.springframework.boot:spring-boot-starter-web")
        implementation("org.springframework.boot:spring-boot-starter-security")

        // jakarta
        annotationProcessor("jakarta.annotation:jakarta.annotation-api")
        annotationProcessor("jakarta.persistence:jakarta.persistence-api")

        // lombok
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        // mapstruct
        implementation("org.mapstruct:mapstruct:${mapstructVersion}")
        annotationProcessor("org.mapstruct:mapstruct-processor:${mapstructVersion}")

        // querydsl
        implementation ("com.querydsl:querydsl-jpa:${querydslVersion}")
        annotationProcessor("com.querydsl:querydsl-apt:${querydslVersion}")

        // database
        runtimeOnly("com.h2database:h2")
        runtimeOnly("com.mysql:mysql-connector-j")

        // jwt
        implementation("io.jsonwebtoken:jjwt-api:0.11.2")
        implementation("io.jsonwebtoken:jjwt-impl:0.11.2")
        implementation("io.jsonwebtoken:jjwt-jackson:0.11.2")

        // springdoc
        implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.2")

        // test
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}