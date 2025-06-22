package aichat.core.repository

import aichat.core.models.LawyerRequest
import aichat.core.models.User
import aichat.core.enums.LawyerRequestStatus
import aichat.core.enums.LawyerRequestUrgency
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface LawyerRequestRepository : JpaRepository<LawyerRequest, Long> {
    // Find a lawyer request by ID
    fun findById(id: Long): Optional<LawyerRequest>
    
    // Find all lawyer requests for a specific user
    fun findByUser(user: User, pageable: Pageable): Page<LawyerRequest>
    
    // Find all lawyer requests with a specific status
    fun findByStatus(status: LawyerRequestStatus, pageable: Pageable): Page<LawyerRequest>
    
    // Find all lawyer requests with a specific urgency
    fun findByUrgency(urgency: LawyerRequestUrgency, pageable: Pageable): Page<LawyerRequest>
    
    // Find all lawyer requests assigned to a specific lawyer
    fun findByAssignedLawyer(lawyer: User, pageable: Pageable): Page<LawyerRequest>
    
    // Find all lawyer requests with a specific status and urgency
    fun findByStatusAndUrgency(status: LawyerRequestStatus, urgency: LawyerRequestUrgency, pageable: Pageable): Page<LawyerRequest>
    
    // Find all lawyer requests for a specific user with a specific status
    fun findByUserAndStatus(user: User, status: LawyerRequestStatus, pageable: Pageable): Page<LawyerRequest>
}