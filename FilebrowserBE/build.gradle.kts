import org.springframework.boot.gradle.plugin.SpringBootPlugin
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.jib)
    jacoco
}

group = "com.anjo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(SpringBootPlugin.BOM_COORDINATES))
    implementation(libs.spring.boot.starter.webmvc)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.flyway)
    implementation(libs.flyway.postgresql)
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.java.time)
    runtimeOnly(libs.postgresql)
    developmentOnly(platform(SpringBootPlugin.BOM_COORDINATES))
    developmentOnly(libs.spring.boot.docker.compose)
    implementation(libs.spring.boot.starter.resource.server)
    implementation(libs.coroutines.core)
    implementation(libs.micrometer.prometheus)
    implementation(libs.micrometer.tracing.otel)
    implementation(libs.spring.boot.tracing.otel)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(platform(libs.grpc.bom))
    implementation(libs.spring.boot.starter.grpc.server)
    implementation(project(":FilebrowserProto"))
    implementation(libs.kotlin.logging)
    implementation(libs.springdoc.openapi)

    testImplementation(libs.spring.boot.starter.webmvc.test)
    testImplementation(libs.spring.boot.micrometer.metrics.test)
    testImplementation(libs.spring.boot.starter.grpc.server.test)
    testImplementation(libs.spring.boot.starter.resource.server.test)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.springmockk)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.property)
    testImplementation(libs.kotest.runner)
    testImplementation(libs.kotest.spring)
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

ktlint {
    version.set(libs.versions.ktlint.engine)
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
            limit {
                counter = "BRANCH"
                minimum = "0.75".toBigDecimal()
            }
        }
    }
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}

tasks.register<BootRun>("runLocal") {
    group = "application"
    description = "Runs the REST API without OIDC: .devops/application-local.yaml over application.yaml"
    mainClass = tasks.bootRun.flatMap { it.mainClass }
    classpath = tasks.bootRun.get().classpath
    args("--spring.config.additional-location=file:${rootProject.file(".devops/application-local.yaml")}")
}

tasks.register<BootRun>("runLocalGrpc") {
    group = "application"
    description = "Runs the gRPC API without OIDC as user local: .devops/application-local.yaml over application.yaml"
    mainClass = tasks.bootRun.flatMap { it.mainClass }
    classpath = tasks.bootRun.get().classpath
    args(
        "--spring.config.additional-location=file:${rootProject.file(".devops/application-local.yaml")}",
        "--spring.profiles.active=server,local",
    )
}

tasks.bootJar {
    archiveFileName.set("filebrowserapi.jar")
}

val jibExtraDirectory = layout.buildDirectory.dir("jib-extra")
val webClientDirectory = rootProject.layout.projectDirectory.dir("FilebrowserUI/build/dist/wasmJs/productionExecutable")

val prepareJibDataDirectory by tasks.registering {
    val dataDirectory = jibExtraDirectory.map { it.dir("data").asFile }
    outputs.dir(jibExtraDirectory)
    doLast { dataDirectory.get().mkdirs() }
}

tasks.matching { it.name.startsWith("jib") }.configureEach {
    dependsOn(prepareJibDataDirectory, ":FilebrowserUI:wasmJsBrowserDistribution")
}

jib {
    from {
        image = "eclipse-temurin:21-jre"
        platforms {
            platform {
                architecture = "amd64"
                os = "linux"
            }
            platform {
                architecture = "arm64"
                os = "linux"
            }
        }
    }
    to {
        image = "ghcr.io/jagieloadrian/filebrowserapi:${findProperty("imageTag") ?: "local"}"
    }
    container {
        user = "10001:10001"
        ports = listOf("8080", "9090")
        volumes = listOf("/data")
        environment =
            mapOf(
                "SPRING_PROFILES_ACTIVE" to "web",
                "APPLICATION_PATH" to "/data",
                "LOGGING_STRUCTURED_FORMAT_CONSOLE" to "com.anjo.filebrowserapi.logging.JsonLogFormatter",
            )
    }
    extraDirectories {
        paths {
            path {
                setFrom(jibExtraDirectory.get().asFile)
                into = "/"
            }
            path {
                setFrom(webClientDirectory.asFile)
                into = "/app/ui"
            }
        }
        permissions.set(mapOf("/data" to "777"))
    }
}
