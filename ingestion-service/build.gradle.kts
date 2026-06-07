plugins {
    kotlin("jvm")
    id("java")
}

dependencies {
    implementation(project(":common"))
    implementation(project(":storage-service"))

    // Kafka
    implementation("org.apache.kafka:kafka-clients:3.4.0")

    // Web scraping
    implementation("org.jsoup:jsoup:1.15.3")

    // HTTP requests
    implementation("org.apache.httpcomponents.client5:httpclient5:5.2")

    // Local file ingestion
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.0")
    implementation("org.postgresql:postgresql:42.5.1")
    implementation("co.elastic.clients:elasticsearch-java:8.6.2")
    implementation("org.elasticsearch.client:elasticsearch-rest-client:8.6.2")

    // Logging
    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.1")
}

tasks.register<JavaExec>("runLocalFileIngestion") {
    group = "application"
    description = "Import local CSV/JSON files into PostgreSQL and Elasticsearch/OpenSearch"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.publicrecord.ingestion.local.LocalFileIngestionMainKt")
    if (project.hasProperty("inputDir")) {
        args(project.property("inputDir").toString())
    }
}

tasks.register<JavaExec>("runCongressGovBills") {
    group = "application"
    description = "Fetch recent Congress.gov bill events and optionally publish them to Kafka"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.publicrecord.ingestion.CongressGovIngestionMainKt")
}

tasks.register<JavaExec>("runCongressGovMembers") {
    group = "application"
    description = "Fetch Congress.gov member profiles into PostgreSQL"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.publicrecord.ingestion.CongressMemberIngestionMainKt")
}

tasks.register<JavaExec>("runGovInfoPackages") {
    group = "application"
    description = "Fetch recent GovInfo package summaries and optionally publish them to Kafka"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.publicrecord.ingestion.GovInfoIngestionMainKt")
}

tasks.register<JavaExec>("runRawContentLogger") {
    group = "application"
    description = "Consume raw-content Kafka events and print them for local verification"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.publicrecord.ingestion.KafkaRawContentLoggerMainKt")
}

tasks.register<JavaExec>("runOfficialDataNormalization") {
    group = "application"
    description = "Fetch official source data and normalize it into PostgreSQL"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.publicrecord.ingestion.OfficialDataNormalizeMainKt")
}

tasks.register<JavaExec>("runStateCivicIngestion") {
    group = "application"
    description = "Fetch Open States and Google Civic records into PostgreSQL"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.publicrecord.ingestion.StateCivicIngestionMainKt")
}

tasks.register<JavaExec>("runMediaIngestion") {
    group = "application"
    description = "Fetch public media discovery records from GDELT, RSS feeds, and YouTube Data API"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.publicrecord.ingestion.MediaIngestionMainKt")
}
