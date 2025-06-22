package aichat.core.models

import aichat.core.enums.LawyerRequestStatus
import aichat.core.enums.LawyerRequestUrgency
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "lawyer_requests")
data class LawyerRequest(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    var id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "client_name", nullable = false)
    var clientName: String,

    @Column(name = "client_email", nullable = false)
    var clientEmail: String,

    @Column(name = "client_phone", nullable = false)
    var clientPhone: String,

    @Column(name = "problem_type", nullable = false)
    var problemType: String,

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    var description: String,

    @Column(name = "preferred_contact_time", nullable = false)
    var preferredContactTime: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency", nullable = false)
    var urgency: LawyerRequestUrgency = LawyerRequestUrgency.NORMAL,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: LawyerRequestStatus = LawyerRequestStatus.PENDING,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_lawyer_id")
    var assignedLawyer: User? = null,

    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null
) {
    constructor() : this(
        id = 0,
        user = User(),
        clientName = "",
        clientEmail = "",
        clientPhone = "",
        problemType = "",
        description = "",
        preferredContactTime = "",
        urgency = LawyerRequestUrgency.NORMAL,
        status = LawyerRequestStatus.PENDING,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        assignedLawyer = null,
        notes = null
    )
}