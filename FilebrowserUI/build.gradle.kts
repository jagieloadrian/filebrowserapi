import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.koin.compiler)
    alias(libs.plugins.buildkonfig)
    alias(libs.plugins.ktlint)
}

group = "com.anjo"
version = "1.0-SNAPSHOT"

repositories {
    google { mavenContent { includeGroupAndSubgroups("androidx") } }
    mavenCentral()
}

val transport = providers.gradleProperty("transport").getOrElse("http")
require(transport in setOf("http", "grpc")) { "Unknown transport '$transport': use -Ptransport=http or -Ptransport=grpc." }
val backendUrl = providers.gradleProperty("backendUrl")
val desktopBackendUrl = backendUrl.getOrElse(if (transport == "grpc") "http://localhost:9090" else "http://localhost:8080")
val oidcIssuer = providers.gradleProperty("oidcIssuer").getOrElse("")
val oidcClientId = providers.gradleProperty("oidcClientId").getOrElse("filebrowser")
val oidcScopes = providers.gradleProperty("oidcScopes").getOrElse("openid profile groups offline_access")
val restProxy = KotlinWebpackConfig.DevServer.Proxy(mutableListOf("/api", "/share"), "http://localhost:8080")

kotlin {
    jvmToolchain(21)
    compilerOptions {
        allWarningsAsErrors = true
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "filebrowser.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).copy(port = 8081, proxy = mutableListOf(restProxy))
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons)
            implementation(libs.lifecycle.viewmodel.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.annotations)
            implementation(libs.ktor.client.core)
            implementation(libs.serialization.json)
        }
        named("desktopMain") {
            dependencies {
                implementation(compose.desktop.currentOs)
                implementation(libs.coroutines.swing)
                implementation(libs.ktor.client.cio)
                implementation(project(":FilebrowserProto"))
                implementation(project.dependencies.platform(libs.grpc.bom))
                implementation(libs.grpc.okhttp)
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(libs.kotest.runner)
                implementation(libs.kotest.assertions)
                implementation(libs.ktor.client.mock)
                implementation(libs.coroutines.test)
                implementation(libs.grpc.inprocess)
            }
        }
        wasmJsMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.kotlinx.browser)
        }
    }
}

koinCompiler {
    versionCheckSeverity = "info"
}

buildkonfig {
    packageName = "com.anjo.filebrowser.ui"
    defaultConfigs {
        buildConfigField(STRING, "backendUrl", backendUrl.getOrElse(""))
        buildConfigField(STRING, "oidcIssuer", oidcIssuer)
        buildConfigField(STRING, "oidcClientId", oidcClientId)
        buildConfigField(STRING, "oidcScopes", oidcScopes)
    }
    targetConfigs {
        create("desktop") {
            buildConfigField(STRING, "transport", transport)
            buildConfigField(STRING, "backendUrl", desktopBackendUrl)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.anjo.filebrowser.ui.MainKt"
        nativeDistributions {
            modules("java.instrument", "java.management", "jdk.unsupported", "jdk.httpserver")
        }
    }
}

ktlint {
    version.set(libs.versions.ktlint.engine)
    filter {
        exclude { it.file.path.contains("/build/") }
    }
}

tasks.named<Test>("desktopTest") {
    useJUnitPlatform()
}
