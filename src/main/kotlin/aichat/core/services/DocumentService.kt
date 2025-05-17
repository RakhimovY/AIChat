package aichat.core.services

import aichat.core.models.Chat
import aichat.core.models.Document
import aichat.core.repository.DocumentRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.charset.StandardCharsets

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val minioService: MinioService,
    @Value("\${minio.bucket-name}") private val bucketName: String
) {
    /**
     * Save a document to MinIO and store metadata in the database
     * @param file The uploaded file
     * @param chat The chat the document belongs to
     * @return The saved document
     * @throws IOException If there's an error reading the file
     */
    @Throws(IOException::class)
    fun saveDocument(file: MultipartFile, chat: Chat): Document {
        // Generate a unique object name for MinIO
        val objectName = "${chat.id}-${System.currentTimeMillis()}-${file.originalFilename}"

        // Upload file to MinIO
        minioService.uploadFile(file, objectName)

        // Create and save document metadata
        val document = Document(
            name = file.originalFilename ?: "unnamed_file",
            contentType = file.contentType ?: "application/octet-stream",
            size = file.size,
            objectName = objectName,
            bucketName = bucketName,
            chat = chat
        )
        return documentRepository.save(document)
    }

    /**
     * Get all documents for a chat
     * @param chatId The ID of the chat
     * @return List of documents
     */
    fun getDocumentsByChatId(chatId: Long): List<Document> {
        return documentRepository.findByChat_Id(chatId)
    }

    /**
     * Get a document by ID
     * @param id The document ID
     * @return The document or null if not found
     */
    fun getDocumentById(id: Long): Document? {
        return documentRepository.findById(id).orElse(null)
    }

    /**
     * Extract text content from a document for AI processing
     * This is a simple implementation that works with text files
     * For more complex document types (PDF, DOCX, etc.), additional libraries would be needed
     */
    fun extractTextContent(document: Document): String {
        try {
            // Download the file from MinIO
            val inputStream = minioService.downloadFile(document.objectName)

            return when {
                document.contentType.startsWith("text/") -> {
                    // For text files, read the content as a string
                    inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
                }
                // For PDF, DOCX, etc., you would use specialized libraries
                // This is a placeholder for future implementation
                else -> {
                    "Document content could not be extracted. File type: ${document.contentType}"
                }
            }
        } catch (e: Exception) {
            return "Error extracting document content: ${e.message}"
        }
    }

    /**
     * Get a presigned URL for downloading a document
     * @param document The document
     * @return The presigned URL
     */
    fun getDocumentUrl(document: Document): String {
        return minioService.getPresignedUrl(document.objectName)
    }
}