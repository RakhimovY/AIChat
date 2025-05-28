package aichat.core.services.web

import aichat.core.models.web.Template
import aichat.core.models.web.WebDocument
import aichat.core.models.web.WebDocumentDto
import aichat.core.repository.UserRepository
import aichat.core.repository.web.WebDocumentRepository
import aichat.core.repository.web.WebTemplateRepository
import aichat.core.services.MinioService
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType1Font
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.util.*

@Service
class WebDocumentService(
    private val webDocumentRepository: WebDocumentRepository,
    private val webTemplateRepository: WebTemplateRepository,
    private val userRepository: UserRepository,
    private val minioService: MinioService,
    private val objectMapper: ObjectMapper
) {
    private val logger = LoggerFactory.getLogger(WebDocumentService::class.java)

    /**
     * Get all documents for a user
     * @param username The username
     * @return List of documents for the user
     */
    @Transactional(readOnly = true)
    fun getAllDocumentsForUser(username: String): List<WebDocument> {
        logger.info("Getting all documents for user: $username")
        val user = userRepository.getUserByEmail(username)
            .orElseThrow { IllegalArgumentException("User not found: $username") }

        return webDocumentRepository.findByUser(user)
    }

    /**
     * Get a document by ID and user
     * @param id The document ID
     * @param username The username
     * @return The document or null if not found
     */
    @Transactional(readOnly = true)
    fun getDocumentByIdAndUser(id: String, username: String): WebDocument? {
        logger.info("Getting document with ID: $id for user: $username")
        val user = userRepository.getUserByEmail(username)
            .orElseThrow { IllegalArgumentException("User not found: $username") }

        return webDocumentRepository.findByIdAndUser(id, user).orElse(null)
    }

    /**
     * Create a new document
     * @param request The document creation request
     * @param username The username
     * @return The created document
     */
    @Transactional
    fun createDocument(request: DocumentCreationRequest, username: String): WebDocument {
        logger.info("Creating document for user: $username with template: ${request.templateId}")
        val user = userRepository.getUserByEmail(username)
            .orElseThrow { IllegalArgumentException("User not found: $username") }

        val template = webTemplateRepository.findById(request.templateId).orElse(null)
            ?: throw IllegalArgumentException("Template not found: ${request.templateId}")

        // Generate a title if not provided
        val title = request.title ?: "${template.title} - ${LocalDateTime.now()}"

        // Create the document
        val document = WebDocument(
            id = UUID.randomUUID().toString(),
            title = title,
            user = user,
            template = template,
            values = objectMapper.writeValueAsString(request.values),
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        return webDocumentRepository.save(document)
    }

    /**
     * Update an existing document
     * @param id The document ID
     * @param request The document update request
     * @param username The username
     * @return The updated document or null if not found
     */
    @Transactional
    fun updateDocument(id: String, request: DocumentUpdateRequest, username: String): WebDocument? {
        logger.info("Updating document with ID: $id for user: $username")
        val user = userRepository.getUserByEmail(username)
            .orElseThrow { IllegalArgumentException("User not found: $username") }

        val existingDocument = webDocumentRepository.findByIdAndUser(id, user).orElse(null)
            ?: return null

        // Update the document
        val updatedDocument = WebDocument(
            id = existingDocument.id,
            title = request.title ?: existingDocument.title,
            user = existingDocument.user,
            template = existingDocument.template,
            values = objectMapper.writeValueAsString(
                request.values ?: objectMapper.readValue<Map<String, Any>>(
                    existingDocument.values
                )
            ),
            createdAt = existingDocument.createdAt,
            updatedAt = LocalDateTime.now()
        )

        return webDocumentRepository.save(updatedDocument)
    }

    /**
     * Delete a document
     * @param id The document ID
     * @param username The username
     * @return True if the document was deleted, false if not found
     */
    @Transactional
    fun deleteDocument(id: String, username: String): Boolean {
        logger.info("Deleting document with ID: $id for user: $username")
        val user = userRepository.getUserByEmail(username)
            .orElseThrow { IllegalArgumentException("User not found: $username") }

        val document = webDocumentRepository.findByIdAndUser(id, user).orElse(null)
            ?: return false

        webDocumentRepository.delete(document)
        return true
    }

    /**
     * Export a document as PDF or DOCX
     * @param request The document export request
     * @param username The username
     * @return Triple of (file bytes, file name, content type)
     */
    @Transactional(readOnly = true)
    fun exportDocument(request: DocumentExportRequest, username: String): Triple<ByteArray, String, String> {
        logger.info("Exporting document with template: ${request.templateId} for user: $username in format: ${request.format}")
        userRepository.getUserByEmail(username)
            .orElseThrow { IllegalArgumentException("User not found: $username") }

        val template = webTemplateRepository.findById(request.templateId).orElse(null)
            ?: throw IllegalArgumentException("Template not found: ${request.templateId}")

        // Generate document content
        val content = generateDocumentContent(template, request.values)

        // Export in the requested format
        return when (request.format.lowercase()) {
            "pdf" -> exportAsPdf(content, template.title)
            "docx" -> exportAsDocx(content, template.title)
            else -> throw IllegalArgumentException("Unsupported format: ${request.format}")
        }
    }

    /**
     * Generate document content by replacing placeholders in the template with values
     * @param template The template
     * @param values The values
     * @return The generated document content
     */
    private fun generateDocumentContent(template: Template, values: Map<String, Any>): String {
        return template.content.replace(Regex("\\{\\{([^}]+)\\}\\}")) { matchResult ->
            val key = matchResult.groupValues[1].trim()
            val value = values[key]?.toString() ?: matchResult.value
            value
        }
    }

    /**
     * Export document as PDF
     * @param content The document content
     * @param title The document title
     * @return Triple of (file bytes, file name, content type)
     */
    private fun exportAsPdf(content: String, title: String): Triple<ByteArray, String, String> {
        val document = PDDocument()
        val page = PDPage()
        document.addPage(page)

        PDPageContentStream(document, page).use { contentStream ->
            contentStream.beginText()
            contentStream.setFont(PDType1Font.HELVETICA, 12f)
            contentStream.newLineAtOffset(50f, 700f)

            // Split content into lines and add to PDF
            val lines = content.split("\n")
            for (line in lines) {
                contentStream.showText(line)
                contentStream.newLineAtOffset(0f, -15f)
            }

            contentStream.endText()
        }

        val outputStream = ByteArrayOutputStream()
        document.save(outputStream)
        document.close()

        val fileName = "$title.pdf"
        val contentType = "application/pdf"

        return Triple(outputStream.toByteArray(), fileName, contentType)
    }

    /**
     * Export document as DOCX
     * @param content The document content
     * @param title The document title
     * @return Triple of (file bytes, file name, content type)
     */
    private fun exportAsDocx(content: String, title: String): Triple<ByteArray, String, String> {
        val document = XWPFDocument()

        // Split content into paragraphs and add to DOCX
        val paragraphs = content.split("\n\n")
        for (paragraphText in paragraphs) {
            if (paragraphText.isBlank()) continue

            val paragraph: XWPFParagraph = document.createParagraph()
            val run: XWPFRun = paragraph.createRun()
            run.setText(paragraphText)
            run.fontFamily = "Calibri"
            run.fontSize = 11
        }

        val outputStream = ByteArrayOutputStream()
        document.write(outputStream)
        document.close()

        val fileName = "$title.docx"
        val contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"

        return Triple(outputStream.toByteArray(), fileName, contentType)
    }

    /**
     * Convert WebDocument entity to DTO
     * @param document The document entity
     * @return The document DTO
     */
    fun toDto(document: WebDocument): WebDocumentDto {
        val values: Map<String, Any> = try {
            objectMapper.readValue<Map<String, Any>>(document.values)
        } catch (e: Exception) {
            logger.error("Error parsing document values: ${e.message}")
            emptyMap()
        }

        return WebDocumentDto.fromEntity(document, values)
    }

    // Request/response classes

    data class DocumentCreationRequest(
        val templateId: String,
        val title: String? = null,
        val values: Map<String, Any>
    )

    data class DocumentUpdateRequest(
        val title: String? = null,
        val values: Map<String, Any>? = null
    )

    data class DocumentExportRequest(
        val templateId: String,
        val values: Map<String, Any>,
        val format: String // "pdf" or "docx"
    )
}
