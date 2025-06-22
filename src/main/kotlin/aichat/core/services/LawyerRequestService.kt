package aichat.core.services

import aichat.core.dto.*
import aichat.core.enums.LawyerRequestStatus
import aichat.core.enums.LawyerRequestUrgency
import aichat.core.models.LawyerRequest
import aichat.core.models.User
import aichat.core.repository.LawyerRequestRepository
import aichat.core.repository.UserRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.*

@Service
class LawyerRequestService(
    private val lawyerRequestRepository: LawyerRequestRepository,
    private val userRepository: UserRepository
) {
    /**
     * Create a new lawyer request
     */
    @Transactional
    fun createLawyerRequest(userId: Long, dto: CreateLawyerRequestDto): LawyerRequestDto {
        val user = userRepository.getUserById(userId)
            .orElseThrow { NoSuchElementException("User not found with ID: $userId") }

        val urgency = try {
            LawyerRequestUrgency.valueOf(dto.urgency.uppercase())
        } catch (e: IllegalArgumentException) {
            LawyerRequestUrgency.NORMAL
        }

        val lawyerRequest = LawyerRequest(
            user = user,
            clientName = dto.clientName,
            clientEmail = dto.clientEmail,
            clientPhone = dto.clientPhone,
            problemType = dto.problemType,
            description = dto.description,
            preferredContactTime = dto.preferredContactTime,
            urgency = urgency,
            status = LawyerRequestStatus.PENDING,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        val savedRequest = lawyerRequestRepository.save(lawyerRequest)
        return LawyerRequestDto.fromEntity(savedRequest)
    }

    /**
     * Get a lawyer request by ID
     */
    fun getLawyerRequestById(id: Long): LawyerRequestDto {
        val lawyerRequest = lawyerRequestRepository.findById(id)
            .orElseThrow { NoSuchElementException("Lawyer request not found with ID: $id") }
        return LawyerRequestDto.fromEntity(lawyerRequest)
    }

    /**
     * Update a lawyer request
     */
    @Transactional
    fun updateLawyerRequest(id: Long, dto: UpdateLawyerRequestDto): LawyerRequestDto {
        val lawyerRequest = lawyerRequestRepository.findById(id)
            .orElseThrow { NoSuchElementException("Lawyer request not found with ID: $id") }

        // Update fields if provided
        dto.clientName?.let { lawyerRequest.clientName = it }
        dto.clientEmail?.let { lawyerRequest.clientEmail = it }
        dto.clientPhone?.let { lawyerRequest.clientPhone = it }
        dto.problemType?.let { lawyerRequest.problemType = it }
        dto.description?.let { lawyerRequest.description = it }
        dto.preferredContactTime?.let { lawyerRequest.preferredContactTime = it }
        dto.notes?.let { lawyerRequest.notes = it }

        // Update urgency if provided
        dto.urgency?.let {
            try {
                lawyerRequest.urgency = LawyerRequestUrgency.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                // Ignore invalid urgency
            }
        }

        // Update status if provided
        dto.status?.let {
            try {
                lawyerRequest.status = LawyerRequestStatus.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                // Ignore invalid status
            }
        }

        // Update assigned lawyer if provided
        dto.assignedLawyerId?.let {
            val lawyer = userRepository.getUserById(it)
            if (lawyer.isPresent) {
                lawyerRequest.assignedLawyer = lawyer.get()
            }
        }

        lawyerRequest.updatedAt = LocalDateTime.now()
        val updatedRequest = lawyerRequestRepository.save(lawyerRequest)
        return LawyerRequestDto.fromEntity(updatedRequest)
    }

    /**
     * Delete a lawyer request
     */
    @Transactional
    fun deleteLawyerRequest(id: Long) {
        if (!lawyerRequestRepository.existsById(id)) {
            throw NoSuchElementException("Lawyer request not found with ID: $id")
        }
        lawyerRequestRepository.deleteById(id)
    }

    /**
     * Cancel a lawyer request
     */
    @Transactional
    fun cancelLawyerRequest(id: Long): LawyerRequestDto {
        val lawyerRequest = lawyerRequestRepository.findById(id)
            .orElseThrow { NoSuchElementException("Lawyer request not found with ID: $id") }

        lawyerRequest.status = LawyerRequestStatus.CANCELLED
        lawyerRequest.updatedAt = LocalDateTime.now()

        val updatedRequest = lawyerRequestRepository.save(lawyerRequest)
        return LawyerRequestDto.fromEntity(updatedRequest)
    }

    /**
     * Get all lawyer requests with filtering and pagination
     */
    fun getLawyerRequests(
        userId: Long? = null,
        status: String? = null,
        urgency: String? = null,
        assignedLawyerId: Long? = null,
        page: Int = 0,
        size: Int = 10
    ): LawyerRequestListResponse {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        
        // Convert status string to enum if provided
        val statusEnum = status?.let {
            try {
                LawyerRequestStatus.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }
        
        // Convert urgency string to enum if provided
        val urgencyEnum = urgency?.let {
            try {
                LawyerRequestUrgency.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        // Apply filters based on provided parameters
        val result: Page<LawyerRequest> = when {
            userId != null && statusEnum != null -> {
                val user = userRepository.getUserById(userId).orElse(null)
                if (user != null) {
                    lawyerRequestRepository.findByUserAndStatus(user, statusEnum, pageable)
                } else {
                    Page.empty(pageable)
                }
            }
            statusEnum != null && urgencyEnum != null -> {
                lawyerRequestRepository.findByStatusAndUrgency(statusEnum, urgencyEnum, pageable)
            }
            userId != null -> {
                val user = userRepository.getUserById(userId).orElse(null)
                if (user != null) {
                    lawyerRequestRepository.findByUser(user, pageable)
                } else {
                    Page.empty(pageable)
                }
            }
            statusEnum != null -> {
                lawyerRequestRepository.findByStatus(statusEnum, pageable)
            }
            urgencyEnum != null -> {
                lawyerRequestRepository.findByUrgency(urgencyEnum, pageable)
            }
            assignedLawyerId != null -> {
                val lawyer = userRepository.getUserById(assignedLawyerId).orElse(null)
                if (lawyer != null) {
                    lawyerRequestRepository.findByAssignedLawyer(lawyer, pageable)
                } else {
                    Page.empty(pageable)
                }
            }
            else -> {
                lawyerRequestRepository.findAll(pageable)
            }
        }

        val dtos = result.content.map { LawyerRequestDto.fromEntity(it) }
        
        return LawyerRequestListResponse(
            content = dtos,
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = page,
            size = size
        )
    }

    /**
     * Assign a lawyer to a request
     */
    @Transactional
    fun assignLawyerToRequest(requestId: Long, lawyerId: Long): LawyerRequestDto {
        val lawyerRequest = lawyerRequestRepository.findById(requestId)
            .orElseThrow { NoSuchElementException("Lawyer request not found with ID: $requestId") }
        
        val lawyer = userRepository.getUserById(lawyerId)
            .orElseThrow { NoSuchElementException("Lawyer not found with ID: $lawyerId") }
        
        lawyerRequest.assignedLawyer = lawyer
        lawyerRequest.status = LawyerRequestStatus.IN_PROGRESS
        lawyerRequest.updatedAt = LocalDateTime.now()
        
        val updatedRequest = lawyerRequestRepository.save(lawyerRequest)
        return LawyerRequestDto.fromEntity(updatedRequest)
    }
}