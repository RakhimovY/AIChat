package aichat.core.models

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "passwokrd_reset_toens")
data class PasswordResetToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @Column(name = "token", nullable = false, unique = true)
    var token: String = UUID.randomUUID().toString(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "expiry_date", nullable = false)
    var expiryDate: LocalDateTime = LocalDateTime.now().plusHours(24),

    @Column(name = "used", nullable = false)
    var used: Boolean = false
) {
    // Default constructor for JPA
    constructor() : this(
        id = 0,
        token = UUID.randomUUID().toString(),
        user = User(), // This will be replaced by JPA during entity loading
        expiryDate = LocalDateTime.now().plusHours(24),
        used = false
    )

    fun isExpired(): Boolean {
        return LocalDateTime.now().isAfter(expiryDate)
    }
}