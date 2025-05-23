package aichat.core.repository

import aichat.core.models.Subscription
import aichat.core.models.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface SubscriptionRepository : JpaRepository<Subscription, Long> {
    fun findByUser(user: User): List<Subscription>
    fun findByPolarSubscriptionId(polarSubscriptionId: String): Optional<Subscription>
    fun findByUserAndStatus(user: User, status: String): List<Subscription>
    fun findTopByUserOrderByCreatedAtDesc(user: User): Optional<Subscription>
}