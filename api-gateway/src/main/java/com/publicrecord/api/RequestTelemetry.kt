package com.publicrecord.api

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object RequestTelemetry {
    private val startedAt = Instant.now()
    private val pathCounts = ConcurrentHashMap<String, AtomicLong>()
    private val statusCounts = ConcurrentHashMap<String, AtomicLong>()
    private val rateLimited = AtomicLong(0)

    fun record(path: String, status: Int) {
        pathCounts.computeIfAbsent(normalize(path)) { AtomicLong(0) }.incrementAndGet()
        statusCounts.computeIfAbsent(status.toString()) { AtomicLong(0) }.incrementAndGet()
    }

    fun recordRateLimited(path: String) {
        rateLimited.incrementAndGet()
        pathCounts.computeIfAbsent("${normalize(path)}:rate_limited") { AtomicLong(0) }.incrementAndGet()
    }

    fun snapshot(): Map<String, Any> {
        return mapOf(
            "startedAt" to startedAt.toString(),
            "pathCounts" to pathCounts.mapValues { it.value.get() }.toSortedMap(),
            "statusCounts" to statusCounts.mapValues { it.value.get() }.toSortedMap(),
            "rateLimitedRequests" to rateLimited.get()
        )
    }

    private fun normalize(path: String): String {
        val clean = path.trim('/').ifBlank { "root" }
        return when {
            clean.startsWith("search") -> "search"
            clean.startsWith("politicians/search") -> "politicians/search"
            clean.startsWith("bills/search") -> "bills/search"
            clean.startsWith("citations") -> "citations"
            clean.startsWith("review") -> "review"
            clean.startsWith("imports") -> "imports"
            else -> clean.split("/").first()
        }
    }
}
