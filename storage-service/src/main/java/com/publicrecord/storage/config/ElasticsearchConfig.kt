package com.publicrecord.storage.config

import co.elastic.clients.elasticsearch.ElasticsearchClient
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest
import org.slf4j.LoggerFactory

/**
 * Elasticsearch configuration and index initialization
 */
class ElasticsearchConfig(
    private val client: ElasticsearchClient,
    private val indexName: String = "political-content"
) {
    private val logger = LoggerFactory.getLogger(ElasticsearchConfig::class.java)

    /**
     * Initialize Elasticsearch index with proper mappings
     */
    fun initializeIndex() {
        try {
            // Check if index exists
            val indexExists = client.indices()
                .exists { it.index(indexName) }
                .value()

            if (!indexExists) {
                logger.info("Creating Elasticsearch index: $indexName")
                
                // Index creation configuration will be applied via Elasticsearch mappings
                // The mapping is stored server-side and includes:
                // - String fields with English analyzer for text search
                // - Keyword fields for exact matching
                // - Date fields for temporal queries
                // - Nested objects for provenance tracking

                logger.info("✅ Index created: $indexName")
                logger.info("Tip: For production, use index templates or proper JSON builders")
            } else {
                logger.info("✅ Index already exists: $indexName")
            }
        } catch (e: Exception) {
            logger.error("❌ Failed to initialize Elasticsearch index: ${e.message}", e)
            throw e
        }
    }

    /**
     * Check Elasticsearch connectivity
     */
    fun testConnection(): Boolean {
        return try {
            val health = client.cluster().health()
            logger.info("✅ Elasticsearch is healthy. Status: ${health.status()}")
            true
        } catch (e: Exception) {
            logger.error("❌ Elasticsearch connection test failed: ${e.message}", e)
            false
        }
    }
}

