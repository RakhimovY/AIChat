package aichat.core.models

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "subscriptions")
data class Subscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "polar_subscription_id", nullable = false)
    var polarSubscriptionId: String,

    @Column(name = "status", nullable = false)
    var status: String,

    @Column(name = "plan_id", nullable = false)
    var planId: String,

    @Column(name = "plan_name", nullable = false)
    var planName: String,

    @Column(name = "current_period_start", nullable = false)
    var currentPeriodStart: LocalDateTime,

    @Column(name = "current_period_end", nullable = false)
    var currentPeriodEnd: LocalDateTime,

    @Column(name = "cancel_at_period_end", nullable = false)
    var cancelAtPeriodEnd: Boolean = false,

    @Column(name = "canceled_at", nullable = true)
    var canceledAt: LocalDateTime? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    constructor() : this(
        id = 0,
        user = User(),
        polarSubscriptionId = "",
        status = "",
        planId = "",
        planName = "",
        currentPeriodStart = LocalDateTime.now(),
        currentPeriodEnd = LocalDateTime.now(),
        cancelAtPeriodEnd = false,
        canceledAt = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}