package aichat.core.services

import aichat.core.models.Chat
import aichat.core.models.Document
import aichat.core.repository.DocumentRepository
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.xwpf.extractor.XWPFWordExtractor
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets

@Service
class DocumentService(
    private val documentRepository: DocumentRepository,
    private val minioService: MinioService,
    @Value("\${minio.bucket-name}") private val bucketName: String
) {
    private val logger = LoggerFactory.getLogger(DocumentService::class.java)
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
     * Supports text files, PDF documents, and Word documents (DOCX, DOC)
     */
    fun extractTextContent(document: Document): String {
        try {
            // Download the file from MinIO
            val inputStream = minioService.downloadFile(document.objectName)

            logger.info("Extracting content from document: ${document.name}, type: ${document.contentType}")

            return when {
                // Text files
                document.contentType.startsWith("text/") -> {
                    logger.info("Processing text file")
                    extractTextFromTextFile(inputStream)
                }

                // PDF files
                document.contentType == "application/pdf" -> {
                    logger.info("Processing PDF file")
                    extractTextFromPdf(inputStream)
                }

                // Word documents - DOCX
                document.contentType == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                    logger.info("Processing DOCX file")
                    extractTextFromDocx(inputStream)
                }

                // Word documents - DOC
                document.contentType == "application/msword" -> {
                    logger.info("Processing DOC file")
                    extractTextFromDoc(inputStream)
                }

                // Unsupported file type
                else -> {
                    logger.warn("Unsupported file type: ${document.contentType}")
                    "Document content could not be extracted. Unsupported file type: ${document.contentType}"
                }
            }
        } catch (e: Exception) {
            logger.error("Error extracting document content", e)
            return "Error extracting document content: ${e.message}"
        }
    }

    /**
     * Extract text from a text file
     */
    private fun extractTextFromTextFile(inputStream: InputStream): String {
        return inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }

    /**
     * Extract text from a PDF file using Apache PDFBox
     */
    private fun extractTextFromPdf(inputStream: InputStream): String {
        return PDDocument.load(inputStream).use { document ->
            val stripper = PDFTextStripper()
            stripper.sortByPosition = true
            stripper.getText(document)
        }
    }

    /**
     * Extract text from a DOCX file using Apache POI
     */
    private fun extractTextFromDocx(inputStream: InputStream): String {
        return XWPFDocument(inputStream).use { document ->
            XWPFWordExtractor(document).use { extractor ->
                extractor.text
            }
        }
    }

    /**
     * Extract text from a DOC file using Apache POI
     */
    private fun extractTextFromDoc(inputStream: InputStream): String {
        return HWPFDocument(inputStream).use { document ->
            document.text.toString()
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
