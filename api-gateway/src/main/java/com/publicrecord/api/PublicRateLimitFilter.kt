package com.publicrecord.api

import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import javax.annotation.Priority
import javax.ws.rs.Priorities
import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.container.ContainerRequestFilter
import javax.ws.rs.core.Response

@Priority(Priorities.AUTHORIZATION)
class PublicRateLimitFilter @JvmOverloads constructor(
    private val enabled: Boolean,
    private val requestsPerMinute: Int,
    private val clock: Clock = Clock.systemUTC()
) : ContainerRequestFilter {
    private val logger = LoggerFactory.getLogger(PublicRateLimitFilter::class.java)
    private val buckets = ConcurrentHashMap<String, Bucket>()
    private val protectedPrefixes = listOf("search", "politicians/search", "bills/search", "citations")

    override fun filter(requestContext: ContainerRequestContext) {
        if (!enabled || !isLimitedPath(requestContext.uriInfo.path.trimStart('/'))) {
            return
        }

        val now = clock.instant()
        val key = "${clientKey(requestContext)}:${requestContext.uriInfo.path.trimStart('/')}"
        val allowed = buckets.compute(key) { _, current ->
            val bucket = current?.takeIf { it.windowStartedAt.plusSeconds(60).isAfter(now) } ?: Bucket(now, 0)
            bucket.copy(count = bucket.count + 1)
        } ?: Bucket(now, 1)

        if (allowed.count > requestsPerMinute) {
            RequestTelemetry.recordRateLimited(requestContext.uriInfo.path)
            logger.warn(
                "Rate limit exceeded path={} client={} limit={}",
                requestContext.uriInfo.path,
                clientKey(requestContext),
                requestsPerMinute
            )
            requestContext.abortWith(
                Response.status(429)
                    .header("Retry-After", "60")
                    .entity(mapOf("error" to "rate_limited", "message" to "Too many requests. Please try again in a minute."))
                    .build()
            )
        }
    }

    private fun isLimitedPath(path: String): Boolean {
        return protectedPrefixes.any { path == it || path.startsWith("$it/") }
    }

    private fun clientKey(requestContext: ContainerRequestContext): String {
        val forwardedFor = requestContext.getHeaderString("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return forwardedFor
            ?: requestContext.getHeaderString("X-Real-IP")?.takeIf { it.isNotBlank() }
            ?: requestContext.securityContext?.userPrincipal?.name
            ?: "anonymous"
    }

    private data class Bucket(
        val windowStartedAt: Instant,
        val count: Int
    )
}
