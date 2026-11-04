plugins {
    val kotlinVersion = "2.0.21"
    kotlin("jvm") version kotlinVersion
}

group = "com.anjo"
version = "1.0-SNPASHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}