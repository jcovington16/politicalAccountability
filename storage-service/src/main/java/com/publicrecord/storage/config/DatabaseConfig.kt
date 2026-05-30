package com.publicrecord.storage.config

import java.sql.Connection
import java.sql.DriverManager
import org.slf4j.LoggerFactory

/**
 * Database configuration and schema initialization
 */
class DatabaseConfig(
    val url: String,
    val username: String,
    val password: String,
    val maxConnections: Int = 10
) {
    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)

    /**
     * Initialize database schema with required tables
     */
    fun initializeSchema() {
        try {
            val connection = getConnection()
            connection.use { conn ->
                logger.info("Initializing database schema...")
                
                // Create politicians table
                conn.createStatement().use { stmt ->
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS politicians (
                            id UUID PRIMARY KEY,
                            first_name VARCHAR(100) NOT NULL,
                            last_name VARCHAR(100) NOT NULL,
                            party VARCHAR(100),
                            state VARCHAR(50),
                            office VARCHAR(100),
                            biography TEXT,
                            profile_image_url TEXT,
                            start_date DATE NOT NULL,
                            end_date DATE,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """.trimIndent())
                    logger.info("✅ Politicians table ready")
                }

                // Create content_items table
                conn.createStatement().use { stmt ->
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS content_items (
                            id UUID PRIMARY KEY,
                            title TEXT NOT NULL,
                            content_type VARCHAR(50) NOT NULL,
                            text_body TEXT,
                            media_url TEXT,
                            published_at TIMESTAMP NOT NULL,
                            content_hash VARCHAR(256) UNIQUE,
                            source_url TEXT NOT NULL,
                            politician_id UUID NOT NULL,
                            keywords TEXT[],
                            tags TEXT[],
                            indexed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (politician_id) REFERENCES politicians (id) ON DELETE CASCADE,
                            INDEX idx_politician_id (politician_id),
                            INDEX idx_published_at (published_at),
                            INDEX idx_content_hash (content_hash)
                        )
                    """.trimIndent())
                    logger.info("✅ Content items table ready")
                }

                // Create provenance table
                conn.createStatement().use { stmt ->
                    stmt.execute("""
                        CREATE TABLE IF NOT EXISTS provenance (
                            id UUID PRIMARY KEY,
                            content_item_id UUID NOT NULL,
                            source_type VARCHAR(100),
                            extractor_version VARCHAR(50),
                            confidence FLOAT,
                            timestamp TIMESTAMP NOT NULL,
                            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                            FOREIGN KEY (content_item_id) REFERENCES content_items (id) ON DELETE CASCADE,
                            INDEX idx_source_type (source_type)
                        )
                    """.trimIndent())
                    logger.info("✅ Provenance table ready")
                }

                logger.info("✅ Database schema initialized successfully")
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize database schema: ${e.message}", e)
            throw e
        }
    }

    /**
     * Get a database connection
     */
    fun getConnection(): Connection {
        return DriverManager.getConnection(url, username, password).apply {
            autoCommit = true
        }
    }

    /**
     * Test database connectivity
     */
    fun testConnection(): Boolean {
        return try {
            getConnection().use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.executeQuery("SELECT 1").next()
                }
            }
        } catch (e: Exception) {
            logger.error("❌ Database connection test failed: ${e.message}", e)
            false
        }
    }
}

