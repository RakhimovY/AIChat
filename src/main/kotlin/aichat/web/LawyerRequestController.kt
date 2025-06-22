package aichat.web

import aichat.core.dto.*
import aichat.core.services.LawyerRequestService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("api/lawyer-requests")
class LawyerRequestController(
    private val lawyerRequestService: LawyerRequestService
) {
    /**
     * Create a new lawyer request
     */
    @PostMapping
    fun createLawyerRequest(
        @RequestBody request: CreateLawyerRequestDto,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LawyerRequestDto> {
        // Get user ID from the authenticated user
        val userId = userDetails.username.toLongOrNull()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()

        val createdRequest = lawyerRequestService.createLawyerRequest(userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(createdRequest)
    }

    /**
     * Get a lawyer request by ID
     */
    @GetMapping("/{id}")
    fun getLawyerRequest(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LawyerRequestDto> {
        return try {
            val lawyerRequest = lawyerRequestService.getLawyerRequestById(id)
            ResponseEntity.ok(lawyerRequest)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Update a lawyer request
     */
    @PutMapping("/{id}")
    fun updateLawyerRequest(
        @PathVariable id: Long,
        @RequestBody request: UpdateLawyerRequestDto,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LawyerRequestDto> {
        return try {
            val updatedRequest = lawyerRequestService.updateLawyerRequest(id, request)
            ResponseEntity.ok(updatedRequest)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Delete a lawyer request
     */
    @DeleteMapping("/{id}")
    fun deleteLawyerRequest(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Map<String, String>> {
        return try {
            // For delete operations, we'll just cancel the request instead of actually deleting it
            val cancelledRequest = lawyerRequestService.cancelLawyerRequest(id)
            ResponseEntity.ok(mapOf("message" to "Lawyer request cancelled successfully"))
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get all lawyer requests with filtering and pagination
     */
    @GetMapping
    fun getLawyerRequests(
        @RequestParam(required = false) userId: Long?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) urgency: String?,
        @RequestParam(required = false) assignedLawyerId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LawyerRequestListResponse> {
        // Check if the user is a lawyer (has LAWYER role)
        val isLawyer = userDetails.authorities.any { it.authority == "ROLE_LAWYER" }
        
        // If not a lawyer, only show their own requests
        val effectiveUserId = if (!isLawyer) {
            userDetails.username.toLongOrNull()
        } else {
            userId
        }

        val requests = lawyerRequestService.getLawyerRequests(
            userId = effectiveUserId,
            status = status,
            urgency = urgency,
            assignedLawyerId = assignedLawyerId,
            page = page,
            size = size
        )
        
        return ResponseEntity.ok(requests)
    }

    /**
     * Assign a lawyer to a request
     */
    @PostMapping("/{id}/assign")
    fun assignLawyerToRequest(
        @PathVariable id: Long,
        @RequestParam lawyerId: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LawyerRequestDto> {
        // Check if the user is a lawyer (has LAWYER role)
        val isLawyer = userDetails.authorities.any { it.authority == "ROLE_LAWYER" }
        if (!isLawyer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        
        return try {
            val updatedRequest = lawyerRequestService.assignLawyerToRequest(id, lawyerId)
            ResponseEntity.ok(updatedRequest)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Self-assign a request to the current lawyer
     */
    @PostMapping("/{id}/self-assign")
    fun selfAssignRequest(
        @PathVariable id: Long,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<LawyerRequestDto> {
        // Check if the user is a lawyer (has LAWYER role)
        val isLawyer = userDetails.authorities.any { it.authority == "ROLE_LAWYER" }
        if (!isLawyer) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
        
        // Get the lawyer's ID
        val lawyerId = userDetails.username.toLongOrNull()
            ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        
        return try {
            val updatedRequest = lawyerRequestService.assignLawyerToRequest(id, lawyerId)
            ResponseEntity.ok(updatedRequest)
        } catch (e: NoSuchElementException) {
            ResponseEntity.notFound().build()
        }
    }
}