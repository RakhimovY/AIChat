package aichat.web

import aichat.core.dto.PolarWebhookEvent
import aichat.core.services.SubscriptionService
import aichat.core.services.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("api/polar/webhook")
class PolarWebhookController(
    private val subscriptionService: SubscriptionService,
    private val userService: UserService
) {
    /**
     * Webhook endpoint for Polar subscription events
     * This endpoint receives webhook events directly from Polar
     */
    @PostMapping
    fun handlePolarWebhook(
        @RequestBody event: PolarWebhookEvent,
        @RequestHeader("X-Polar-Signature") signature: String?
    ): ResponseEntity<Map<String, String>> {
        // In a production environment, you should verify the signature
        // to ensure the webhook is coming from Polar

        // Log the webhook event
        println("Received Polar webhook event: $event")

        try {
            when (event.type) {
                "subscription.created" -> handleSubscriptionCreated(event.data)
                "subscription.updated" -> handleSubscriptionUpdated(event.data)
                "subscription.deleted" -> handleSubscriptionDeleted(event.data)
                "subscription.active" -> handleSubscriptionActive(event.data)
                "subscription.canceled" -> handleSubscriptionCanceled(event.data)
                // Add more event types as needed
            }

            return ResponseEntity.ok(mapOf("status" to "success"))
        } catch (e: Exception) {
            println("Error processing Polar webhook: ${e.message}")
            e.printStackTrace()
            return ResponseEntity.internalServerError()
                .body(mapOf("status" to "error", "message" to e.message.orEmpty()))
        }
    }

    private fun handleSubscriptionCreated(data: PolarWebhookEvent.PolarSubscriptionData) {
        val user = userService.getUserByEmail(data.subscriber.email)

        // Parse date strings to LocalDateTime
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val startDate = LocalDateTime.parse(data.currentPeriodStart, formatter)
        val endDate = LocalDateTime.parse(data.currentPeriodEnd, formatter)

        subscriptionService.createSubscription(
            user = user,
            polarSubscriptionId = data.id,
            status = data.status,
            planId = data.plan.id,
            planName = data.plan.name,
            currentPeriodStart = startDate,
            currentPeriodEnd = endDate
        )
    }

    private fun handleSubscriptionUpdated(data: PolarWebhookEvent.PolarSubscriptionData) {
        // Parse date strings to LocalDateTime
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val startDate = LocalDateTime.parse(data.currentPeriodStart, formatter)
        val endDate = LocalDateTime.parse(data.currentPeriodEnd, formatter)
        val canceledAt = if (data.canceledAt != null) LocalDateTime.parse(data.canceledAt, formatter) else null

        subscriptionService.updateSubscription(
            polarSubscriptionId = data.id,
            status = data.status,
            currentPeriodStart = startDate,
            currentPeriodEnd = endDate,
            cancelAtPeriodEnd = data.cancelAtPeriodEnd,
            canceledAt = canceledAt
        )
    }

    private fun handleSubscriptionActive(data: PolarWebhookEvent.PolarSubscriptionData) {
        // Parse date strings to LocalDateTime
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        val startDate = LocalDateTime.parse(data.currentPeriodStart, formatter)
        val endDate = LocalDateTime.parse(data.currentPeriodEnd, formatter)

        subscriptionService.updateSubscription(
            polarSubscriptionId = data.id,
            status = "active",
            currentPeriodStart = startDate,
            currentPeriodEnd = endDate,
            cancelAtPeriodEnd = data.cancelAtPeriodEnd,
            canceledAt = null
        )
    }

    private fun handleSubscriptionCanceled(data: PolarWebhookEvent.PolarSubscriptionData) {
        subscriptionService.cancelSubscription(data.id)
    }

    private fun handleSubscriptionDeleted(data: PolarWebhookEvent.PolarSubscriptionData) {
        // Mark the subscription as canceled
        subscriptionService.cancelSubscription(data.id)
    }
}