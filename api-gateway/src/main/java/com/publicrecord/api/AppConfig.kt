package com.publicrecord.api

import io.dropwizard.Configuration
import com.fasterxml.jackson.annotation.JsonProperty
import javax.validation.constraints.NotEmpty

class AppConfig : Configuration() {
    @NotEmpty
    @JsonProperty("serviceName")
    var serviceName: String = "political-accountability-app"

    @JsonProperty("databaseUrl")
    var databaseUrl: String = envOrDefault("DATABASE_URL", "jdbc:postgresql://postgres:5432/political_data")

    @JsonProperty("databaseUser")
    var databaseUser: String = envOrDefault("DATABASE_USER", "postgres")

    @JsonProperty("databasePassword")
    var databasePassword: String = envOrDefault("DATABASE_PASSWORD", localDevDefault("postgres"))

    @JsonProperty("databaseDriverClass")
    var databaseDriverClass: String = "org.postgresql.Driver"

    @JsonProperty("databaseMaxConnections")
    var databaseMaxConnections: Int = 10

    @JsonProperty("elasticsearchHost")
    var elasticsearchHost: String = envOrDefault("ELASTICSEARCH_URL", "http://elasticsearch:9200")

    @JsonProperty("kafkaBootstrapServers")
    var kafkaBootstrapServers: String = envOrDefault("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")

    @JsonProperty("minioEndpoint")
    var minioEndpoint: String = envOrDefault("MINIO_ENDPOINT", "http://minio:9000")

    @JsonProperty("minioAccessKey")
    var minioAccessKey: String = envOrDefault("MINIO_ACCESS_KEY", localDevDefault("minioadmin"))

    @JsonProperty("minioSecretKey")
    var minioSecretKey: String = envOrDefault("MINIO_SECRET_KEY", localDevDefault("minioadmin"))

    @JsonProperty("minioBucket")
    var minioBucket: String = envOrDefault("MINIO_BUCKET", "political-media")

    @JsonProperty("adminApiToken")
    var adminApiToken: String = envOrDefault("ADMIN_API_TOKEN", localDevDefault("local-admin-token"))

    @JsonProperty("publicSearchRateLimitPerMinute")
    var publicSearchRateLimitPerMinute: Int = envIntOrDefault("PUBLIC_SEARCH_RATE_LIMIT_PER_MINUTE", 60)

    @JsonProperty("rateLimitEnabled")
    var rateLimitEnabled: Boolean = envBoolOrDefault("RATE_LIMIT_ENABLED", true)

    @JsonProperty("cors")
    var cors: CorsConfig = CorsConfig()  // New nested CORS configuration

    fun validateForStartup() {
        if (!isProduction()) {
            return
        }

        require(databasePassword.isNotBlank()) {
            "DATABASE_PASSWORD or databasePassword must be set in production"
        }
        require(minioAccessKey.isNotBlank()) {
            "MINIO_ACCESS_KEY or minioAccessKey must be set in production"
        }
        require(minioSecretKey.isNotBlank()) {
            "MINIO_SECRET_KEY or minioSecretKey must be set in production"
        }
        require(cors.allowedOrigins != "*") {
            "Wildcard CORS origins are not allowed in production"
        }
        require(!cors.allowCredentials || cors.allowedOrigins != "*") {
            "Credentialed CORS cannot use wildcard origins"
        }
        require(adminApiToken.isNotBlank()) {
            "ADMIN_API_TOKEN or adminApiToken must be set in production"
        }
        require(adminApiToken.length >= 24) {
            "ADMIN_API_TOKEN must be at least 24 characters in production"
        }
        require(publicSearchRateLimitPerMinute in 10..600) {
            "PUBLIC_SEARCH_RATE_LIMIT_PER_MINUTE must be between 10 and 600"
        }
    }

    companion object {
        private fun envOrDefault(name: String, defaultValue: String): String {
            return System.getenv(name)?.takeIf { it.isNotBlank() } ?: defaultValue
        }

        private fun envIntOrDefault(name: String, defaultValue: Int): Int {
            return System.getenv(name)?.trim()?.toIntOrNull() ?: defaultValue
        }

        private fun envBoolOrDefault(name: String, defaultValue: Boolean): Boolean {
            return System.getenv(name)?.trim()?.lowercase()?.let {
                it == "true" || it == "1" || it == "yes"
            } ?: defaultValue
        }

        /*
         * Local developer defaults keep docker-compose easy to run. Production
         * should provide these values through environment-specific config.
         */
        private fun localDevDefault(value: String): String {
            return if (System.getenv("APP_ENV") == "production") "" else value
        }

        private fun isProduction(): Boolean {
            return System.getenv("APP_ENV") == "production"
        }
    }
}
