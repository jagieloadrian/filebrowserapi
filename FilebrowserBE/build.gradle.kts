plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.jib)
    jacoco
}

group = "com.anjo"
version = "1.0-SNPASHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.micrometer.prometheus)
    implementation(libs.micrometer.tracing.otel)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.kotlin.reflect)
    implementation(libs.grpc.netty)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.kotlin.stub)
    implementation(libs.protobuf.kotlin)
    implementation(libs.kotlin.logging)
    implementation(libs.springdoc.openapi)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.springmockk)
    testImplementation(libs.kotest.assertions)
    testImplementation(libs.kotest.property)
    testImplementation(libs.coroutines.test)
    testImplementation(kotlin("test"))
}

protobuf {
    protoc {
        artifact =
            libs.protobuf.protoc
                .get()
                .toString()
    }
    plugins {
        create("grpc") {
            artifact =
                libs.grpc.protoc.gen.java
                    .get()
                    .toString()
        }
        create("grpckt") {
            artifact = "${libs.grpc.protoc.gen.kotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                create("grpc")
                create("grpckt")
            }
            it.builtins {
                create("kotlin")
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors = true
    }
}

ktlint {
    filter {
        exclude { it.file.path.contains("/build/generated/") }
    }
}

tasks.test {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

val generatedProtoClasses = "com/anjo/filebrowser/*"

tasks.jacocoTestReport {
    classDirectories.setFrom(classDirectories.files.map { fileTree(it) { exclude(generatedProtoClasses) } })
}

tasks.jacocoTestCoverageVerification {
    classDirectories.setFrom(classDirectories.files.map { fileTree(it) { exclude(generatedProtoClasses) } })
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

tasks.bootJar {
    archiveFileName.set("filebrowserapi.jar")
}

val jibExtraDirectory = layout.buildDirectory.dir("jib-extra")

val prepareJibDataDirectory by tasks.registering {
    val dataDirectory = jibExtraDirectory.map { it.dir("data").asFile }
    outputs.dir(jibExtraDirectory)
    doLast { dataDirectory.get().mkdirs() }
}

tasks.matching { it.name.startsWith("jib") }.configureEach {
    dependsOn(prepareJibDataDirectory)
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
        }
        permissions.set(mapOf("/data" to "777"))
    }
}
