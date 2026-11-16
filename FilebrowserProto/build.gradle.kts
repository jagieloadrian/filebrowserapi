plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.ktlint)
}

group = "com.anjo"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    api(platform(libs.grpc.bom))
    api(libs.grpc.protobuf)
    api(libs.grpc.kotlin.stub)
    api(libs.protobuf.kotlin)
    api(libs.coroutines.core)
}

protobuf {
    protoc {
        artifact =
            libs.protobuf.protoc
                .get()
                .toString()
    }
    plugins {
        maybeCreate("grpc").apply {
            artifact =
                libs.grpc.protoc.gen.java
                    .get()
                    .toString()
        }
        maybeCreate("grpckt").apply {
            artifact = "${libs.grpc.protoc.gen.kotlin.get()}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                maybeCreate("grpc")
                maybeCreate("grpckt")
            }
            it.builtins {
                maybeCreate("kotlin")
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
    version.set(libs.versions.ktlint.engine)
    filter {
        exclude { it.file.path.contains("/build/generated/") }
    }
}
