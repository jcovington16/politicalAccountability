plugins {
    kotlin("jvm")
    id("java")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":storage-service"))

    // Kafka
    implementation("org.apache.kafka:kafka-clients:3.4.0")

    // NLP processing
    implementation("edu.stanford.nlp:stanford-corenlp:4.5.1")

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
}

