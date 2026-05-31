package com.publicrecord.api

import io.dropwizard.Application
import io.dropwizard.setup.Environment
import com.publicrecord.api.resources.PoliticianResource
import com.publicrecord.api.resources.TimelineResource
import com.publicrecord.api.resources.ContentItemResource
import com.publicrecord.api.resources.NewsResource
import com.publicrecord.api.resources.BillResource
import com.publicrecord.api.resources.TrustResource
import com.publicrecord.api.resources.VotingRecordResource
import com.publicrecord.api.services.BillService
import com.publicrecord.api.services.VotingRecordService
import com.publicrecord.storage.services.DatabaseService
import com.publicrecord.storage.services.ElasticSearchService
import com.publicrecord.storage.services.MinIOService
import com.publicrecord.storage.services.KafkaService
import com.publicrecord.storage.services.ProcessedContentSinkService
import com.publicrecord.storage.config.DatabaseConfig
import com.publicrecord.storage.repositories.BillRepository
import com.publicrecord.storage.repositories.ContentItemRepository
import com.publicrecord.storage.repositories.PoliticianRepository
import com.publicrecord.storage.repositories.SourceCitationRepository
import com.publicrecord.storage.repositories.VotingRecordRepository
import com.publicrecord.storage.managed.DatabaseManagedService
import com.publicrecord.storage.managed.ElasticSearchManagedService
import com.publicrecord.storage.managed.MinIOManagedService
import com.publicrecord.storage.managed.KafkaManagedService
import com.publicrecord.storage.managed.ProcessedContentSinkManagedService
import org.slf4j.LoggerFactory
import org.eclipse.jetty.servlets.CrossOriginFilter
import java.util.EnumSet
import javax.servlet.DispatcherType

class App : Application<AppConfig>() {
    private val logger = LoggerFactory.getLogger(App::class.java)

    override fun run(config: AppConfig, env: Environment) {

        logger.info("🚀 Starting Dropwizard API Gateway...")
        config.validateForStartup()

        // Instantiate backend services
        val databaseService = DatabaseService()
        val elasticSearchService = ElasticSearchService()
        val minioService = MinIOService()
        val kafkaService = KafkaService()

        /*
         * Early-stage architecture note:
         *
         * The project still has a storage-service module because the long-term
         * direction is a separate storage microservice. For now, that module is
         * used as an in-process persistence library so the gateway can query the
         * database directly without depending on a storage HTTP server that does
         * not exist yet.
         */
        val databaseConfig = DatabaseConfig(
            url = config.databaseUrl,
            username = config.databaseUser,
            password = config.databasePassword,
            maxConnections = config.databaseMaxConnections
        )
        val politicianRepository = PoliticianRepository(databaseConfig)
        val contentItemRepository = ContentItemRepository(databaseConfig)
        val billRepository = BillRepository(databaseConfig)
        val sourceCitationRepository = SourceCitationRepository(databaseConfig)
        val votingRecordRepository = VotingRecordRepository(databaseConfig)
        val billService = BillService(billRepository, sourceCitationRepository)
        val votingRecordService = VotingRecordService(votingRecordRepository)
        val processedContentSinkService = ProcessedContentSinkService(
            kafkaBootstrapServers = config.kafkaBootstrapServers,
            contentItemRepository = contentItemRepository
        )

        // Register managed services with Dropwizard lifecycle
        env.lifecycle().manage(DatabaseManagedService(databaseService))
        env.lifecycle().manage(ElasticSearchManagedService(elasticSearchService))
        env.lifecycle().manage(MinIOManagedService(minioService))
        env.lifecycle().manage(KafkaManagedService(kafkaService))
        env.lifecycle().manage(ProcessedContentSinkManagedService(processedContentSinkService))

        configureCors(config, env)

        // Register API resources
        env.jersey().register(PoliticianResource(politicianRepository))
        env.jersey().register(TimelineResource(contentItemRepository))
        env.jersey().register(ContentItemResource(contentItemRepository))
        env.jersey().register(NewsResource())
        env.jersey().register(BillResource(billService))
        env.jersey().register(VotingRecordResource(votingRecordService))
        env.jersey().register(TrustResource())

        logger.info("✅ Dropwizard API Gateway started successfully!")
        logger.info("📍 API available at: http://localhost:8080")
        logger.info("📊 Admin interface available at: http://localhost:8081")
    }

    private fun configureCors(config: AppConfig, env: Environment) {
        val cors = env.servlets().addFilter("cors", CrossOriginFilter::class.java)
        cors.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, config.cors.allowedOrigins)
        cors.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, config.cors.allowedMethods)
        cors.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM, config.cors.allowedHeaders)
        cors.setInitParameter(CrossOriginFilter.ALLOW_CREDENTIALS_PARAM, config.cors.allowCredentials.toString())
        cors.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType::class.java), true, "/*")

        logger.info("CORS configured for origins={}", config.cors.allowedOrigins)
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            App().run("server", "api-gateway/src/main/resources/config.yml")
        }
    }
}
