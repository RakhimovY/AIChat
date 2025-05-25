package aichat.core.utils

import org.slf4j.Logger
import kotlin.math.pow

/**
 * Utility class for retry operations with exponential backoff
 */
class RetryUtils {
    companion object {
        /**
         * Executes an operation with retry logic using exponential backoff and jitter
         * 
         * @param operation The operation to execute
         * @param maxRetries Maximum number of retry attempts
         * @param initialBackoffMs Initial backoff time in milliseconds
         * @param logger Logger for logging retry attempts
         * @param operationName Name of the operation for logging
         * @param contextInfo Additional context information for logging
         * @return The result of the operation
         * @throws Exception If all retry attempts fail
         */
        suspend fun <T> withRetry(
            operation: suspend () -> T,
            maxRetries: Int = 5,
            initialBackoffMs: Long = 2000,
            logger: Logger,
            operationName: String,
            contextInfo: String
        ): T {
            var retryCount = 0
            var lastException: Exception? = null

            while (retryCount < maxRetries) {
                try {
                    // If this is a retry, log it
                    if (retryCount > 0) {
                        logger.info("Retry attempt $retryCount for $operationName ($contextInfo)")
                    }

                    // Call the operation
                    return operation()
                } catch (e: Exception) {
                    lastException = e

                    // Check if it's a connection reset error
                    val isConnectionReset = e.message?.contains("Connection reset") == true || 
                                           e.cause?.message?.contains("Connection reset") == true

                    if (isConnectionReset) {
                        logger.error("Connection reset error calling $operationName (attempt ${retryCount + 1}). This is usually a temporary network issue.", e)
                    } else {
                        logger.error("Error calling $operationName (attempt ${retryCount + 1}): ${e.message}", e)
                    }

                    // If we've reached max retries, throw the last exception
                    if (retryCount >= maxRetries - 1) {
                        logger.error("All retry attempts failed for $operationName ($contextInfo)", e)
                        throw e
                    }

                    // Exponential backoff with jitter: wait longer between each retry with some randomness
                    val baseBackoffTime = (initialBackoffMs * 2.0.pow(retryCount.toDouble())).toLong()
                    // Add jitter (±20% randomness) to prevent thundering herd problem
                    val jitterFactor = 0.8 + Math.random() * 0.4 // Random value between 0.8 and 1.2
                    val backoffTime = (baseBackoffTime * jitterFactor).toLong()

                    logger.info("Waiting ${backoffTime}ms before retry")
                    Thread.sleep(backoffTime)
                    retryCount++
                }
            }

            // This should never be reached due to the throw in the loop, but Kotlin requires it
            throw lastException ?: IllegalStateException("Unknown error in retry mechanism")
        }

        /**
         * Non-suspending version of withRetry for use in non-coroutine contexts
         */
        fun <T> withRetryBlocking(
            operation: () -> T,
            maxRetries: Int = 5,
            initialBackoffMs: Long = 2000,
            logger: Logger,
            operationName: String,
            contextInfo: String
        ): T {
            var retryCount = 0
            var lastException: Exception? = null

            while (retryCount < maxRetries) {
                try {
                    // If this is a retry, log it
                    if (retryCount > 0) {
                        logger.info("Retry attempt $retryCount for $operationName ($contextInfo)")
                    }

                    // Call the operation
                    return operation()
                } catch (e: Exception) {
                    lastException = e

                    // Check if it's a connection reset error
                    val isConnectionReset = e.message?.contains("Connection reset") == true || 
                                           e.cause?.message?.contains("Connection reset") == true

                    if (isConnectionReset) {
                        logger.error("Connection reset error calling $operationName (attempt ${retryCount + 1}). This is usually a temporary network issue.", e)
                    } else {
                        logger.error("Error calling $operationName (attempt ${retryCount + 1}): ${e.message}", e)
                    }

                    // If we've reached max retries, throw the last exception
                    if (retryCount >= maxRetries - 1) {
                        logger.error("All retry attempts failed for $operationName ($contextInfo)", e)
                        throw e
                    }

                    // Exponential backoff with jitter: wait longer between each retry with some randomness
                    val baseBackoffTime = (initialBackoffMs * 2.0.pow(retryCount.toDouble())).toLong()
                    // Add jitter (±20% randomness) to prevent thundering herd problem
                    val jitterFactor = 0.8 + Math.random() * 0.4 // Random value between 0.8 and 1.2
                    val backoffTime = (baseBackoffTime * jitterFactor).toLong()

                    logger.info("Waiting ${backoffTime}ms before retry")
                    Thread.sleep(backoffTime)
                    retryCount++
                }
            }

            // This should never be reached due to the throw in the loop, but Kotlin requires it
            throw lastException ?: IllegalStateException("Unknown error in retry mechanism")
        }
    }
}