plugins {
    kotlin("jvm")
}

dependencies {
    implementation("org.apache.kafka:kafka-clients:3.4.0")

    implementation("org.json:json:20210307")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
}

