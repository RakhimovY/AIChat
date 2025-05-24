package aichat.core.dto

/**
 * Request for subscription notification
 */
data class SubscriptionNotificationRequest(
    val polarSubscriptionId: String,
    val email: String? = null,
    val status: String? = null,
    val planId: String? = null,
    val planName: String? = null
)

/**
 * Response for subscription notification
 */
data class SubscriptionNotificationResponse(
    val success: Boolean,
    val message: String,
    val subscription: SubscriptionResponse?
)

/**
 * Request for subscription cancellation
 */
data class SubscriptionCancellationRequest(
    val polarSubscriptionId: String
)

/**
 * Response for subscription cancellation
 */
data class SubscriptionCancellationResponse(
    val success: Boolean,
    val message: String,
    val subscription: SubscriptionResponse?
)

