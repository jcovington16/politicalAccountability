plugins {
    kotlin("jvm")
    id("java") // ✅ Added to ensure Java dependencies work
}

dependencies {
    implementation(project(":common"))
    implementation(project(":storage-service"))
    implementation(project(":event-streaming"))

    // Add khttp for HTTP client functionality
    implementation("com.squareup.okhttp3:okhttp:4.10.0")

    // Dropwizard dependencies
    implementation("io.dropwizard:dropwizard-core:2.1.6")
    implementation("io.dropwizard:dropwizard-jetty:2.1.6")         // <-- This is critical
    implementation("io.dropwizard:dropwizard-jackson:2.1.6")
    implementation("io.dropwizard:dropwizard-configuration:2.1.6")

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

    // ✅ Add Kafka Streams if Needed
    implementation("org.apache.kafka:kafka-streams:3.3.1")

    // Logging
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // Jackson Kotlin module for JSON parsing
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
    testImplementation("io.dropwizard:dropwizard-testing:2.1.6")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

tasks.register<JavaExec>("runApiGateway") {
    group = "application"
    description = "Run the Dropwizard API gateway with local configuration"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.publicrecord.api.App")
}
