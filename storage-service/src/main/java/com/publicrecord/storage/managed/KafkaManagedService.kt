package com.publicrecord.storage.managed

import io.dropwizard.lifecycle.Managed
import com.publicrecord.storage.services.KafkaService
import org.slf4j.LoggerFactory

class KafkaManagedService(private val kafkaService: KafkaService) : Managed {
    private val logger = LoggerFactory.getLogger(KafkaManagedService::class.java)

    override fun start() {
        logger.info("Starting Kafka Service...")
        kafkaService.startService()
        logger.info("Kafka Service started successfully.")
    }

    override fun stop() {
        logger.info("Stopping Kafka Service...")
        kafkaService.stop()
        logger.info("Kafka Service stopped successfully.")
    }
}