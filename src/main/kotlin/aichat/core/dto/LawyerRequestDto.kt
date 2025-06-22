package aichat.core.dto

import aichat.core.enums.LawyerRequestStatus
import aichat.core.enums.LawyerRequestUrgency
import aichat.core.models.LawyerRequest
import java.time.LocalDateTime

data class LawyerRequestDto(
    val id: Long,
    val userId: Long,
    val clientName: String,
    val clientEmail: String,
    val clientPhone: String,
    val problemType: String,
    val description: String,
    val preferredContactTime: String,
    val urgency: LawyerRequestUrgency,
    val status: LawyerRequestStatus,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val assignedLawyerId: Long? = null,
    val notes: String? = null
) {
    companion object {
        fun fromEntity(entity: LawyerRequest): LawyerRequestDto {
            return LawyerRequestDto(
                id = entity.id,
                userId = entity.user.id,
                clientName = entity.clientName,
                clientEmail = entity.clientEmail,
                clientPhone = entity.clientPhone,
                problemType = entity.problemType,
                description = entity.description,
                preferredContactTime = entity.preferredContactTime,
                urgency = entity.urgency,
                status = entity.status,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                assignedLawyerId = entity.assignedLawyer?.id,
                notes = entity.notes
            )
        }
    }
}

// DTO for creating a new lawyer request
data class CreateLawyerRequestDto(
    val clientName: String,
    val clientEmail: String,
    val clientPhone: String,
    val problemType: String,
    val description: String,
    val preferredContactTime: String,
    val urgency: String = "NORMAL"
)

// DTO for updating a lawyer request
data class UpdateLawyerRequestDto(
    val clientName: String? = null,
    val clientEmail: String? = null,
    val clientPhone: String? = null,
    val problemType: String? = null,
    val description: String? = null,
    val preferredContactTime: String? = null,
    val urgency: String? = null,
    val status: String? = null,
    val assignedLawyerId: Long? = null,
    val notes: String? = null
)

// DTO for lawyer request list response
data class LawyerRequestListResponse(
    val content: List<LawyerRequestDto>,
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val size: Int
)