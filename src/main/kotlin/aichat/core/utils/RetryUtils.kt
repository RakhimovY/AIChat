package aichat.core.utils

import org.slf4j.Logger
import kotlin.math.pow

/**
 * Utility class for retry operations with exponential backoff
 */
object RetryUtils {
    /**
     * Execute a function with retry and exponential backoff
     *
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay in milliseconds
     * @param logger Logger for logging retry attempts and errors
     * @param operation The operation to execute with retry
     * @return The result of the operation
     * @throws Exception The last exception encountered if all retries fail
     */
    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        logger: Logger,
        contextInfo: String = "",
        operation: suspend () -> T
    ): T {
        var retryCount = 0
        var lastException: Exception? = null

        while (retryCount < maxRetries) {
            try {
                // If this is a retry, log it
                if (retryCount > 0) {
                    logger.info("Retry attempt $retryCount $contextInfo")
                }

                // Execute the operation
                return operation()
            } catch (e: Exception) {
                lastException = e
                logger.error("Error during operation (attempt ${retryCount + 1}): ${e.message} $contextInfo", e)

                // If we've reached max retries, break
                if (retryCount >= maxRetries - 1) {
                    break
                }

                // Exponential backoff: wait longer between each retry
                val backoffTime = (initialDelayMs * 2.0.pow(retryCount.toDouble())).toLong()
                logger.info("Waiting ${backoffTime}ms before retry")
                kotlinx.coroutines.delay(backoffTime)
                retryCount++
            }
        }

        // If all retries failed, throw the last exception
        throw lastException ?: RuntimeException("All retry attempts failed $contextInfo")
    }

    /**
     * Execute a function with retry and exponential backoff (blocking version)
     *
     * @param maxRetries Maximum number of retry attempts
     * @param initialDelayMs Initial delay in milliseconds
     * @param logger Logger for logging retry attempts and errors
     * @param operation The operation to execute with retry
     * @return The result of the operation
     * @throws Exception The last exception encountered if all retries fail
     */
    fun <T> withRetryBlocking(
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000,
        logger: Logger,
        contextInfo: String = "",
        operation: () -> T
    ): T {
        var retryCount = 0
        var lastException: Exception? = null

        while (retryCount < maxRetries) {
            try {
                // If this is a retry, log it
                if (retryCount > 0) {
                    logger.info("Retry attempt $retryCount $contextInfo")
                }

                // Execute the operation
                return operation()
            } catch (e: Exception) {
                lastException = e
                logger.error("Error during operation (attempt ${retryCount + 1}): ${e.message} $contextInfo", e)

                // If we've reached max retries, break
                if (retryCount >= maxRetries - 1) {
                    break
                }

                // Exponential backoff: wait longer between each retry
                val backoffTime = (initialDelayMs * 2.0.pow(retryCount.toDouble())).toLong()
                logger.info("Waiting ${backoffTime}ms before retry")
                Thread.sleep(backoffTime)
                retryCount++
            }
        }

        // If all retries failed, throw the last exception
        throw lastException ?: RuntimeException("All retry attempts failed $contextInfo")
    }
}