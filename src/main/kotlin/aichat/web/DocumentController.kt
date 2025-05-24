package aichat.web

import aichat.core.services.DocumentService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RestController
@RequestMapping("api/documents")
class DocumentController(
    private val documentService: DocumentService
) {
    /**
     * Download a document by ID
     * @param id The document ID
     * @return A redirect to a presigned URL for downloading the document
     */
    @GetMapping("/{id}")
    fun downloadDocument(@PathVariable id: Long): ResponseEntity<Void> {
        val document = documentService.getDocumentById(id)
            ?: return ResponseEntity.notFound().build()

        // Generate a presigned URL for downloading the document
        val presignedUrl = documentService.getDocumentUrl(document)

        // Redirect to the presigned URL
        return ResponseEntity.status(HttpStatus.FOUND)
            .location(URI.create(presignedUrl))
            .build()
    }
}
