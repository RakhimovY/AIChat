package aichat.web.controllers

import aichat.core.models.web.WebDocumentDto
import aichat.core.services.web.WebDocumentService
import aichat.core.services.web.WebDocumentService.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/web/documents")
class WebDocumentController(
    private val webDocumentService: WebDocumentService
) {
    private val logger = LoggerFactory.getLogger(WebDocumentController::class.java)

    /**
     * Get all documents for the current user
     * @return List of documents
     */
    @GetMapping
    fun getAllDocuments(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<List<WebDocumentDto>> {
        logger.info("Getting all documents for user: ${userDetails.username}")
        
        val documents = webDocumentService.getAllDocumentsForUser(userDetails.username)
        return ResponseEntity.ok(documents.map { webDocumentService.toDto(it) })
    }

    /**
     * Get a document by ID
     * @param id The document ID
     * @return The document or 404 if not found
     */
    @GetMapping("/{id}")
    fun getDocumentById(
        @PathVariable id: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<WebDocumentDto> {
        logger.info("Getting document with ID: $id for user: ${userDetails.username}")
        
        val document = webDocumentService.getDocumentByIdAndUser(id, userDetails.username)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(webDocumentService.toDto(document))
    }

    /**
     * Create a new document
     * @param request The document creation request
     * @return The created document
     */
    @PostMapping
    fun createDocument(
        @RequestBody request: DocumentCreationRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<WebDocumentDto> {
        logger.info("Creating document for user: ${userDetails.username} with template: ${request.templateId}")
        
        val document = webDocumentService.createDocument(request, userDetails.username)
        return ResponseEntity.status(201).body(webDocumentService.toDto(document))
    }

    /**
     * Update an existing document
     * @param id The document ID
     * @param request The document update request
     * @return The updated document or 404 if not found
     */
    @PutMapping("/{id}")
    fun updateDocument(
        @PathVariable id: String,
        @RequestBody request: DocumentUpdateRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<WebDocumentDto> {
        logger.info("Updating document with ID: $id for user: ${userDetails.username}")
        
        val document = webDocumentService.updateDocument(id, request, userDetails.username)
            ?: return ResponseEntity.notFound().build()
        
        return ResponseEntity.ok(webDocumentService.toDto(document))
    }

    /**
     * Delete a document
     * @param id The document ID
     * @return 204 No Content if successful, 404 if not found
     */
    @DeleteMapping("/{id}")
    fun deleteDocument(
        @PathVariable id: String,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<Void> {
        logger.info("Deleting document with ID: $id for user: ${userDetails.username}")
        
        val deleted = webDocumentService.deleteDocument(id, userDetails.username)
        return if (deleted) ResponseEntity.noContent().build() else ResponseEntity.notFound().build()
    }

    /**
     * Export a document as PDF or DOCX
     * @param request The document export request
     * @return The exported document file
     */
    @PostMapping("/export")
    fun exportDocument(
        @RequestBody request: DocumentExportRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<ByteArray> {
        logger.info("Exporting document with template: ${request.templateId} for user: ${userDetails.username} in format: ${request.format}")
        
        val (fileBytes, fileName, contentType) = webDocumentService.exportDocument(request, userDetails.username)
        
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(contentType))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .body(fileBytes)
    }
}