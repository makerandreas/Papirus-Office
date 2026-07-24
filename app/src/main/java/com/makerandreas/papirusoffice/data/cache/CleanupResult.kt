package com.makerandreas.papirusoffice.data.cache

/**
 * Result data holder for automated cache cleanup operations.
 */
data class CleanupResult(
    val purgedCount: Int,
    val success: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
