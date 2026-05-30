package com.publicrecord.storage.managed

import com.publicrecord.storage.services.ProcessedContentSinkService
import io.dropwizard.lifecycle.Managed
import org.slf4j.LoggerFactory

class ProcessedContentSinkManagedService(
    private val sinkService: ProcessedContentSinkService
) : Managed {
    private val logger = LoggerFactory.getLogger(ProcessedContentSinkManagedService::class.java)

    override fun start() {
        logger.info("Starting processed content sink managed service")
        sinkService.start()
    }

    override fun stop() {
        logger.info("Stopping processed content sink managed service")
        sinkService.stop()
    }
}
