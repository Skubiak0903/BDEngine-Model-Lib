plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    id("io.freefair.lombok") version "9.2.0"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    implementation("net.minestom:minestom:2026.03.03-1.21.11")
    
    implementation("org.slf4j:slf4j-api:2.0.17") // logger
    implementation("org.joml:joml:1.10.8") // quaternion math
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}