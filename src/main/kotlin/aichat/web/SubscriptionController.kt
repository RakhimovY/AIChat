package aichat.web

import aichat.core.dto.*
import aichat.core.models.Subscription
import aichat.core.services.SubscriptionService
import aichat.core.services.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("api/subscriptions")
class SubscriptionController(
    private val subscriptionService: SubscriptionService,
    private val userService: UserService
) {
    /**
     * Check subscription status for the authenticated user
     * Returns whether the user has an active subscription and the list of subscriptions
     */
    @GetMapping("/status")
    fun checkSubscriptionStatus(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Map<String, Any>> {
        val user = userService.getUserByEmail(userDetails.username)
        val hasActiveSubscription = subscriptionService.hasActiveSubscription(user)
        val subscriptions = subscriptionService.getUserSubscriptions(user)

        return ResponseEntity.ok(mapOf(
            "hasActiveSubscription" to hasActiveSubscription,
            "subscriptions" to subscriptions.map { SubscriptionResponse.fromSubscription(it) },
            "message" to if (hasActiveSubscription) "User has active subscription" else "User does not have active subscription"
        ))
    }
    /**
     * Get all subscriptions for the authenticated user
     */
    @GetMapping
    fun getUserSubscriptions(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<SubscriptionResponse>> {
        val user = userService.getUserByEmail(userDetails.username)
        val subscriptions = subscriptionService.getUserSubscriptions(user)
        return ResponseEntity.ok(subscriptions.map { SubscriptionResponse.fromSubscription(it) })
    }

    /**
     * Get active subscriptions for the authenticated user
     */
    @GetMapping("/active")
    fun getActiveSubscriptions(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<SubscriptionResponse>> {
        val user = userService.getUserByEmail(userDetails.username)
        val subscriptions = subscriptionService.getActiveSubscriptions(user)
        return ResponseEntity.ok(subscriptions.map { SubscriptionResponse.fromSubscription(it) })
    }

    /**
     * Cancel a subscription
     */
    @PostMapping("/cancel")
    fun cancelSubscription(
        @RequestBody request: SubscriptionCancellationRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<SubscriptionCancellationResponse> {
        val user = userService.getUserByEmail(userDetails.username)

        // Verify that the subscription belongs to the authenticated user
        val subscriptions = subscriptionService.getUserSubscriptions(user)
        val subscription = subscriptions.find { it.polarSubscriptionId == request.polarSubscriptionId }

        if (subscription == null) {
            return ResponseEntity.badRequest().body(
                SubscriptionCancellationResponse(
                    success = false,
                    message = "Subscription not found or does not belong to the authenticated user",
                    subscription = null
                )
            )
        }

        val canceledSubscription = subscriptionService.cancelSubscription(request.polarSubscriptionId)

        return if (canceledSubscription != null) {
            ResponseEntity.ok(
                SubscriptionCancellationResponse(
                    success = true,
                    message = "Subscription canceled successfully",
                    subscription = SubscriptionResponse.fromSubscription(canceledSubscription)
                )
            )
        } else {
            ResponseEntity.badRequest().body(
                SubscriptionCancellationResponse(
                    success = false,
                    message = "Failed to cancel subscription",
                    subscription = null
                )
            )
        }
    }

    // Webhook handling has been moved to PolarWebhookController


    /**
     * Manual notification endpoint for subscription events
     * This is used when the webhook might not be received (e.g., during development)
     */
    @PostMapping("/notify")
    fun notifySubscription(
        @RequestBody request: SubscriptionNotificationRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<SubscriptionNotificationResponse> {
        try {
            // Get the user from the authentication or the request
            val email = request.email ?: userDetails.username
            val user = userService.getUserByEmail(email)

            // Check if the subscription already exists
            val existingSubscriptions = subscriptionService.getUserSubscriptions(user)
            val existingSubscription = existingSubscriptions.find { it.polarSubscriptionId == request.polarSubscriptionId }

            if (existingSubscription != null) {
                // If the subscription exists and we have a status update, update it
                if (request.status != null && request.status != existingSubscription.status) {
                    val updatedSubscription = subscriptionService.updateSubscription(
                        polarSubscriptionId = existingSubscription.polarSubscriptionId,
                        status = request.status,
                        currentPeriodStart = existingSubscription.currentPeriodStart,
                        currentPeriodEnd = existingSubscription.currentPeriodEnd,
                        cancelAtPeriodEnd = existingSubscription.cancelAtPeriodEnd,
                        canceledAt = existingSubscription.canceledAt
                    )

                    return ResponseEntity.ok(
                        SubscriptionNotificationResponse(
                            success = true,
                            message = "Subscription status updated to ${request.status}",
                            subscription = updatedSubscription?.let { SubscriptionResponse.fromSubscription(it) }
                        )
                    )
                }

                // Subscription already exists and no status change, return it
                return ResponseEntity.ok(
                    SubscriptionNotificationResponse(
                        success = true,
                        message = "Subscription already exists",
                        subscription = SubscriptionResponse.fromSubscription(existingSubscription)
                    )
                )
            }

            // If this is a manual check without a specific subscription ID, return empty response
            if (request.polarSubscriptionId == "manual-check") {
                return ResponseEntity.ok(
                    SubscriptionNotificationResponse(
                        success = true,
                        message = "No active subscription found",
                        subscription = null
                    )
                )
            }

            // Create a new subscription with default values or provided values
            val now = LocalDateTime.now()
            val endDate = now.plusMonths(1) // Assume monthly subscription

            val subscription = subscriptionService.createSubscription(
                user = user,
                polarSubscriptionId = request.polarSubscriptionId,
                status = request.status ?: "active",
                planId = request.planId ?: "premium-monthly", // Default plan ID
                planName = request.planName ?: "Premium Monthly", // Default plan name
                currentPeriodStart = now,
                currentPeriodEnd = endDate
            )

            return ResponseEntity.ok(
                SubscriptionNotificationResponse(
                    success = true,
                    message = "Subscription created successfully",
                    subscription = SubscriptionResponse.fromSubscription(subscription)
                )
            )
        } catch (e: Exception) {
            return ResponseEntity.badRequest().body(
                SubscriptionNotificationResponse(
                    success = false,
                    message = "Failed to create subscription: ${e.message}",
                    subscription = null
                )
            )
        }
    }
}
