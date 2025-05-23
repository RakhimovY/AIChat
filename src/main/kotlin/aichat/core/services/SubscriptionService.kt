package aichat.core.services

import aichat.core.models.Subscription
import aichat.core.models.User
import aichat.core.repository.SubscriptionRepository
import aichat.core.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val userRepository: UserRepository
) {
    /**
     * Create a new subscription for a user
     */
    @Transactional
    fun createSubscription(
        user: User,
        polarSubscriptionId: String,
        status: String,
        planId: String,
        planName: String,
        currentPeriodStart: LocalDateTime,
        currentPeriodEnd: LocalDateTime
    ): Subscription {
        val subscription = Subscription(
            user = user,
            polarSubscriptionId = polarSubscriptionId,
            status = status,
            planId = planId,
            planName = planName,
            currentPeriodStart = currentPeriodStart,
            currentPeriodEnd = currentPeriodEnd
        )
        
        // Update user's subscription information
        user.subscriptionId = polarSubscriptionId
        user.subscriptionStatus = status
        user.role = "PREMIUM" // Upgrade user to premium role
        userRepository.save(user)
        
        return subscriptionRepository.save(subscription)
    }
    
    /**
     * Update an existing subscription
     */
    @Transactional
    fun updateSubscription(
        polarSubscriptionId: String,
        status: String,
        currentPeriodStart: LocalDateTime,
        currentPeriodEnd: LocalDateTime,
        cancelAtPeriodEnd: Boolean,
        canceledAt: LocalDateTime?
    ): Subscription? {
        val subscriptionOpt = subscriptionRepository.findByPolarSubscriptionId(polarSubscriptionId)
        if (subscriptionOpt.isEmpty) {
            return null
        }
        
        val subscription = subscriptionOpt.get()
        subscription.status = status
        subscription.currentPeriodStart = currentPeriodStart
        subscription.currentPeriodEnd = currentPeriodEnd
        subscription.cancelAtPeriodEnd = cancelAtPeriodEnd
        subscription.canceledAt = canceledAt
        subscription.updatedAt = LocalDateTime.now()
        
        // Update user's subscription status
        val user = subscription.user
        user.subscriptionStatus = status
        
        // Update user's role based on subscription status
        if (status == "active") {
            user.role = "PREMIUM"
        } else if (status == "canceled" || status == "expired") {
            user.role = "USER"
        }
        
        userRepository.save(user)
        
        return subscriptionRepository.save(subscription)
    }
    
    /**
     * Cancel a subscription
     */
    @Transactional
    fun cancelSubscription(polarSubscriptionId: String): Subscription? {
        val subscriptionOpt = subscriptionRepository.findByPolarSubscriptionId(polarSubscriptionId)
        if (subscriptionOpt.isEmpty) {
            return null
        }
        
        val subscription = subscriptionOpt.get()
        subscription.cancelAtPeriodEnd = true
        subscription.canceledAt = LocalDateTime.now()
        subscription.updatedAt = LocalDateTime.now()
        
        // We don't immediately change the user's role or status
        // This will happen when the subscription period ends
        
        return subscriptionRepository.save(subscription)
    }
    
    /**
     * Get all subscriptions for a user
     */
    fun getUserSubscriptions(user: User): List<Subscription> {
        return subscriptionRepository.findByUser(user)
    }
    
    /**
     * Get active subscriptions for a user
     */
    fun getActiveSubscriptions(user: User): List<Subscription> {
        return subscriptionRepository.findByUserAndStatus(user, "active")
    }
    
    /**
     * Get the most recent subscription for a user
     */
    fun getLatestSubscription(user: User): Subscription? {
        val subscriptionOpt = subscriptionRepository.findTopByUserOrderByCreatedAtDesc(user)
        return if (subscriptionOpt.isPresent) subscriptionOpt.get() else null
    }
    
    /**
     * Check if a user has an active subscription
     */
    fun hasActiveSubscription(user: User): Boolean {
        return getActiveSubscriptions(user).isNotEmpty()
    }
}