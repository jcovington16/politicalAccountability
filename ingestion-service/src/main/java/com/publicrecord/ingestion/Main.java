package com.publicrecord;

import com.publicrecord.ingestion.IngestionService;
import com.publicrecord.ingestion.connectors.RSSFeedConnector;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 * Ingestion Service Entry Point
 * Starts data ingestion from various sources and publishes to Kafka
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        try {
            logger.info("🚀 Starting Ingestion Service...");
            
            String kafkaBootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
            if (kafkaBootstrapServers == null) {
                kafkaBootstrapServers = "kafka:9092";
            }
            
            // Initialize ingestion service
            IngestionService ingestionService = new IngestionService(kafkaBootstrapServers);
            
            // Register connectors
            ingestionService.registerConnector(new RSSFeedConnector("https://news.example.com/feed", "Example News"));
            
            // Start periodic ingestion (every 5 minutes)
            ingestionService.startPeriodic(300);
            
            // Keep the service running
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down ingestion service...");
                ingestionService.shutdown();
            }));
            
            logger.info("✅ Ingestion Service started successfully!");
            
            // Block indefinitely
            Thread.currentThread().join();
            
        } catch (Exception e) {
            logger.error("❌ Failed to start ingestion service", e);
            System.exit(1);
        }
    }
}