package com.publicrecord.storage.services

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.util.Properties
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class KafkaService {
    private val bootstrapServers = System.getenv("KAFKA_BOOTSTRAP_SERVERS") ?: "kafka:9092"
    private val topicName = "political-events"
    private val topicInitializer = KafkaTopicInitializer(bootstrapServers)

    private val producerProps = Properties().apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer")
    }

    private val producer = KafkaProducer<String, String>(producerProps)
    private val running = AtomicBoolean(false)
    private var consumerThread: Thread? = null

    fun startService() {
        println("✅ Kafka Service Started. bootstrapServers=$bootstrapServers, topic=$topicName")
        // Initialize topics before starting the consumer
        topicInitializer.initializeTopics()
        startConsumer()
    }

    // ... existing code ...
    fun sendMessage(key: String, message: String) {
        val record = ProducerRecord(topicName, key, message)
        producer.send(record)
        println("Sent message: $message")
    }
    // ... existing code ...

    private val consumerProps = Properties().apply {
        put("bootstrap.servers", bootstrapServers)
        put("group.id", "political-group")
        put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer")
        put("auto.offset.reset", "earliest")
        put("enable.auto.commit", "true")
    }
    private val consumer = KafkaConsumer<String, String>(consumerProps)

    private fun startConsumer() {
        running.set(true)
        consumer.subscribe(listOf(topicName))
        consumerThread = thread(isDaemon = true, name = "kafka-consumer-$topicName") {
            println("Kafka consumer thread started.")
            try {
                while (running.get()) {
                    val records = consumer.poll(Duration.ofMillis(1000))
                    for (record in records) {
                        println("Received message: ${record.value()} (key=${record.key()}, partition=${record.partition()}, offset=${record.offset()})")
                        // TODO: Dispatch to processing logic
                    }
                }
            } catch (ex: Exception) {
                println("Kafka consumer error: ${ex.message}")
            } finally {
                try {
                    consumer.close()
                } catch (_: Exception) {}
                println("Kafka consumer closed.")
            }
        }
    }

    fun stop() {
        println("Stopping Kafka Service...")
        running.set(false)
        consumerThread?.join(2000)
        try {
            producer.flush()
            producer.close()
        } catch (_: Exception) {}
        println("Kafka Service stopped.")
    }
}