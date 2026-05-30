package com.publicrecord.storage.services

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.NewTopic
import org.slf4j.LoggerFactory
import java.util.Properties
import java.util.concurrent.TimeUnit

/**
 * Initializes Kafka topics on application startup.
 * Ensures required topics exist before consumers attempt to subscribe.
 */
class KafkaTopicInitializer(
    private val bootstrapServers: String
) {
    private val logger = LoggerFactory.getLogger(KafkaTopicInitializer::class.java)

    companion object {
        // Required topics for the application
        private val REQUIRED_TOPICS = listOf(
            TopicConfig("political-events", 1, 1),
            TopicConfig("raw-content", 1, 1),
            TopicConfig("processed-content", 1, 1)
        )
    }

    data class TopicConfig(
        val name: String,
        val partitions: Int,
        val replicationFactor: Int
    )

    /**
     * Initialize Kafka topics. Creates missing topics and logs existing ones.
     * @throws Exception if AdminClient operations fail critically
     */
    fun initializeTopics() {
        try {
            logger.info("🔍 Initializing Kafka topics on bootstrap servers: $bootstrapServers")

            val props = Properties().apply {
                put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
                put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000)
            }

            val adminClient = AdminClient.create(props)

            try {
                // Get list of existing topics
                val existingTopics = adminClient.listTopics().names().get(30, TimeUnit.SECONDS)
                logger.info("📋 Existing topics: $existingTopics")

                // Determine which topics need to be created
                val topicsToCreate = REQUIRED_TOPICS.filter { !existingTopics.contains(it.name) }

                if (topicsToCreate.isNotEmpty()) {
                    logger.info("🆕 Creating ${topicsToCreate.size} missing topics...")
                    val newTopics = topicsToCreate.map {
                        NewTopic(it.name, it.partitions, it.replicationFactor.toShort())
                    }

                    val createResult = adminClient.createTopics(newTopics)
                    createResult.all().get(30, TimeUnit.SECONDS)

                    logger.info("✅ Successfully created topics: ${topicsToCreate.map { it.name }}")
                } else {
                    logger.info("✅ All required topics already exist")
                }

                // Verify all required topics exist after initialization
                val finalTopics = adminClient.listTopics().names().get(30, TimeUnit.SECONDS)
                val missingTopics = REQUIRED_TOPICS.filter { !finalTopics.contains(it.name) }

                if (missingTopics.isNotEmpty()) {
                    logger.warn("⚠️  Some topics still missing after initialization: $missingTopics")
                } else {
                    logger.info("✅ All required topics are available and ready for use")
                }

            } catch (e: Exception) {
                logger.error("❌ Failed to initialize Kafka topics: ${e.message}", e)
                throw e
            } finally {
                adminClient.close()
            }

        } catch (e: Exception) {
            logger.error("❌ Kafka topic initialization failed. Application may not work correctly: ${e.message}", e)
            // Don't throw - allow app to start, but log the warning
            // Topics may be created later or already exist
        }
    }
}

