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

val backendUrl = providers.gradleProperty("backendUrl").getOrElse("http://localhost:8080")

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
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).copy(port = 8081)
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
            }
        }
        named("desktopTest") {
            dependencies {
                implementation(libs.kotest.runner)
                implementation(libs.kotest.assertions)
                implementation(libs.ktor.client.mock)
                implementation(libs.coroutines.test)
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
        buildConfigField(STRING, "backendUrl", backendUrl)
    }
}

compose.desktop {
    application {
        mainClass = "com.anjo.filebrowser.ui.MainKt"
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
