package com.publicrecord.storage.services

import java.sql.Connection
import java.sql.DriverManager
import org.slf4j.LoggerFactory

class DatabaseService {
    private val logger = LoggerFactory.getLogger(DatabaseService::class.java)

    // Read configuration from environment variables with sensible defaults
    private val dbUrl: String = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://postgres:5432/political_data"
    private val dbUser: String = System.getenv("DATABASE_USER") ?: "postgres"
    private val dbPassword: String = System.getenv("DATABASE_PASSWORD") ?: "postgres"

    private val connection: Connection = try {
        DriverManager.getConnection(dbUrl, dbUser, dbPassword)
    } catch (e: Exception) {
        logger.error("Failed to connect to database at $dbUrl with user $dbUser: ${e.message}", e)
        throw e
    }

    fun startService() {
        logger.info("Starting database service and connection to $dbUrl")
    }

    fun getConnection(): Connection {
        return connection
    }
}
