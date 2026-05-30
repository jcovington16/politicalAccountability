plugins {
    kotlin("jvm")
    id("java")
}

dependencies {
    implementation(project(":common"))

    // PostgreSQL JDBC Driver
    implementation("org.postgresql:postgresql:42.5.1")

    // Kotlin SQL ORM (Exposed)
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")

    // Elasticsearch Java Client
    implementation("co.elastic.clients:elasticsearch-java:8.6.2")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2")

    // MinIO Client (S3-Compatible)
    implementation("io.minio:minio:8.5.7")

    // Kafka Dependencies
    implementation("org.apache.kafka:kafka-clients:3.3.1")
    implementation("org.apache.kafka:kafka-streams:3.3.1")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // **Add Dropwizard lifecycle dependency**
    implementation("io.dropwizard:dropwizard-lifecycle:2.1.6")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testImplementation("org.liquibase:liquibase-core:4.33.0")
}
