plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    `maven-publish`
    id("io.freefair.lombok") version "9.2.0"
}

dependencies {
    implementation("net.minestom:minestom:2026.03.03-1.21.11")
    
    implementation("org.slf4j:slf4j-api:2.0.17") // logger
    implementation("org.joml:joml:1.10.8") // quaternion math
    
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.slf4j:slf4j-simple:2.0.17")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}



tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}