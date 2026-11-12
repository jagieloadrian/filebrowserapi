plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ktlint)
}

group = "com.anjo"
version = "1.0-SNPASHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.actuator)
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
}

tasks.bootJar {
    archiveFileName.set("filebrowserapi.jar")
}
