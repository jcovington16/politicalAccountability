plugins {
    kotlin("jvm") version "1.9.24"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.owasp.dependencycheck") version "12.2.2"
    id("jacoco")
}

allprojects {
    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "kotlin")

    group = "com.publicrecord"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    java { // ✅ Keep only this for Java 21
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    dependencies {
        implementation(kotlin("stdlib"))
        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testImplementation("org.testcontainers:junit-jupiter:1.19.3")
        testImplementation("org.testcontainers:postgresql:1.19.3")
        testImplementation("org.testcontainers:elasticsearch:1.19.3")
        testImplementation("org.testcontainers:kafka:1.19.3")
        testImplementation("io.mockk:mockk:1.13.8")
        testImplementation("org.assertj:assertj-core:3.24.2")
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
        exclude("**/*IntegrationTest.class")
        exclude("**/*IT.class")
    }

    val testSourceSet = extensions.getByType<SourceSetContainer>()["test"]

    tasks.register<Test>("integrationTest") {
        useJUnitPlatform()
        testClassesDirs = testSourceSet.output.classesDirs
        classpath = testSourceSet.runtimeClasspath
        include("**/*IntegrationTest.class")
        include("**/*IT.class")
        shouldRunAfter(tasks.test)
    }
}

dependencies {
    implementation(project(":storage-service"))
    implementation(project(":api-gateway"))

    // Add Dropwizard dependencies to the root project
    implementation("io.dropwizard:dropwizard-core:2.1.4")
    implementation("io.dropwizard:dropwizard-client:2.1.4")
    implementation("io.dropwizard:dropwizard-auth:2.1.4")
    implementation("io.dropwizard:dropwizard-assets:2.1.4")

    // SLF4J API
    implementation("org.slf4j:slf4j-api:1.7.36")
    // Logback Classic (SLF4J implementation) - use 1.2.11 to match Dropwizard compatibility
    implementation("ch.qos.logback:logback-classic:1.2.11")

    // Root-level integration tests live under src/test, so the root project
    // needs its own test dependencies in addition to the subproject defaults.
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:elasticsearch:1.19.3")
    testImplementation("org.testcontainers:kafka:1.19.3")
    testImplementation("org.assertj:assertj-core:3.24.2")
}

tasks.shadowJar {
    archiveFileName.set("political-accountability-app-all.jar")
    manifest {
        // Point shadowJar main class to the API Gateway Dropwizard application
        attributes["Main-Class"] = "com.publicrecord.api.App"
    }
    mergeServiceFiles()  // This is critical to merge META-INF/services files
}



tasks.test {
    useJUnitPlatform()
    // Keep Docker/Testcontainers tests out of the default unit-test task.
    // Run them explicitly with ./gradlew integrationTest when Docker is available.
    exclude("**/*IntegrationTest.class")
    exclude("**/*IT.class")
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

// Integration test task
tasks.register<Test>("integrationTest") {
    dependsOn(subprojects.map { "${it.path}:integrationTest" })
    useJUnitPlatform()
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    
    // Include integration tests
    include("**/*IntegrationTest.class")
    include("**/*IT.class")
    
    // Set test environment variables
    environment("TEST_PROFILE", "integration")
}

// Dependency check configuration
dependencyCheck {
    failBuildOnCVSS = 7.0f
    suppressionFile = "dependency-check-suppressions.xml"
    formats = listOf("HTML", "JSON")

    // Keep the vulnerability database in a stable cache location so CI can
    // restore it between runs. This avoids hammering NVD on every workflow.
    data.directory = "${System.getProperty("user.home")}/.gradle/dependency-check-data"

    // NVD strongly prefers API-keyed access. In CI, set repository secret
    // NVD_API_KEY; local runs continue to work without committing anything.
    System.getenv("NVD_API_KEY")?.takeIf { it.isNotBlank() }?.let { key ->
        nvd.apiKey = key
    }
}

// Task to generate test coverage report
tasks.register("testCoverage") {
    dependsOn("test", "jacocoTestReport")
    doLast {
        println("Test coverage report generated at: build/reports/jacoco/test/html/index.html")
    }
}
