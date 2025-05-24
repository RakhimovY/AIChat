package aichat.core.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Data class representing a webhook event from Polar
 */
data class PolarWebhookEvent(
    val type: String,
    val data: PolarSubscriptionData
) {
    /**
     * Data class representing subscription data in a Polar webhook event
     */
    data class PolarSubscriptionData(
        val id: String,
        val status: String,
        @JsonProperty("current_period_start")
        val currentPeriodStart: String,
        @JsonProperty("current_period_end")
        val currentPeriodEnd: String,
        @JsonProperty("cancel_at_period_end")
        val cancelAtPeriodEnd: Boolean,
        @JsonProperty("canceled_at")
        val canceledAt: String?,
        val plan: PolarPlan,
        val subscriber: PolarSubscriber
    )

    /**
     * Data class representing a plan in Polar subscription data
     */
    data class PolarPlan(
        val id: String,
        val name: String,
        val amount: Int,
        val currency: String,
        val interval: String,
        @JsonProperty("interval_count")
        val intervalCount: Int
    )

    /**
     * Data class representing a subscriber in Polar subscription data
     */
    data class PolarSubscriber(
        val id: String,
        val email: String,
        val name: String?
    )
}