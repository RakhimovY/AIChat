package aichat.core.dto

import aichat.core.models.Subscription
import java.time.LocalDateTime

/**
 * DTO for subscription response
 */
data class SubscriptionResponse(
    val id: Long,
    val polarSubscriptionId: String,
    val status: String,
    val planId: String,
    val planName: String,
    val currentPeriodStart: LocalDateTime,
    val currentPeriodEnd: LocalDateTime,
    val cancelAtPeriodEnd: Boolean,
    val canceledAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
) {
    companion object {
        fun fromSubscription(subscription: Subscription): SubscriptionResponse {
            return SubscriptionResponse(
                id = subscription.id,
                polarSubscriptionId = subscription.polarSubscriptionId,
                status = subscription.status,
                planId = subscription.planId,
                planName = subscription.planName,
                currentPeriodStart = subscription.currentPeriodStart,
                currentPeriodEnd = subscription.currentPeriodEnd,
                cancelAtPeriodEnd = subscription.cancelAtPeriodEnd,
                canceledAt = subscription.canceledAt,
                createdAt = subscription.createdAt,
                updatedAt = subscription.updatedAt
            )
        }
    }
}

