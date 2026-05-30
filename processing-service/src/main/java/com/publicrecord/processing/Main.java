package com.publicrecord;

import com.publicrecord.processing.ProcessingService;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Processing Service Entry Point
 * Consumes raw content from Kafka, enriches it with NLP, and publishes processed content
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            logger.info("🚀 Starting Processing Service...");
            
            String kafkaBootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
            if (kafkaBootstrapServers == null) {
                kafkaBootstrapServers = "kafka:9092";
            }
            
            String elasticsearchHost = System.getenv("ELASTICSEARCH_URL");
            if (elasticsearchHost == null) {
                elasticsearchHost = "http://elasticsearch:9200";
            }
            
            // Initialize processing service
            ProcessingService processingService = new ProcessingService(kafkaBootstrapServers, elasticsearchHost);
            
            // Register enrichers (comment out until enrichers are implemented)
            // processingService.registerEnricher(new NEREnricher());
            // processingService.registerEnricher(new PoliticianLinkerEnricher());
            
            // Start consuming and processing
            processingService.start();
            
            // Keep the service running
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down processing service...");
                processingService.shutdown();
            }));
            
            logger.info("✅ Processing Service started successfully!");
            
            // Block indefinitely
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("❌ Failed to start processing service", e);
            System.exit(1);
        }
    }
}