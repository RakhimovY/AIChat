package aichat.core.services

import aichat.core.models.Chat
import aichat.core.models.Image
import aichat.core.repository.ImageRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException

@Service
class ImageService(
    private val imageRepository: ImageRepository,
    private val minioService: MinioService,
    @Value("\${minio.bucket-name}") private val bucketName: String
) {
    private val logger = LoggerFactory.getLogger(ImageService::class.java)

    /**
     * Save an image to MinIO and store metadata in the database
     * @param file The uploaded image file
     * @param chat The chat the image belongs to
     * @return The saved image
     * @throws IOException If there's an error reading the file
     */
    @Throws(IOException::class)
    fun saveImage(file: MultipartFile, chat: Chat): Image {
        // Generate a unique object name for MinIO
        val objectName = "img-${chat.id}-${System.currentTimeMillis()}-${file.originalFilename}"

        // Upload file to MinIO
        minioService.uploadFile(file, objectName)

        // Create and save image metadata
        val image = Image(
            name = file.originalFilename ?: "unnamed_image",
            contentType = file.contentType ?: "image/jpeg",
            size = file.size,
            objectName = objectName,
            bucketName = bucketName,
            chat = chat
        )
        return imageRepository.save(image)
    }

    /**
     * Get all images for a chat
     * @param chatId The ID of the chat
     * @return List of images
     */
    fun getImagesByChatId(chatId: Long): List<Image> {
        return imageRepository.findByChat_Id(chatId)
    }

    /**
     * Get an image by ID
     * @param id The image ID
     * @return The image or null if not found
     */
    fun getImageById(id: Long): Image? {
        return imageRepository.findById(id).orElse(null)
    }

    /**
     * Get a presigned URL for downloading an image
     * @param image The image
     * @return The presigned URL
     */
    fun getImageUrl(image: Image): String {
        return minioService.getPresignedUrl(image.objectName)
    }

    /**
     * Validate if the file is a supported image type
     * @param file The file to validate
     * @return True if the file is a supported image type, false otherwise
     */
    fun isValidImageType(file: MultipartFile): Boolean {
        val contentType = file.contentType ?: return false
        return contentType.startsWith("image/") && 
               (contentType == "image/jpeg" || 
                contentType == "image/jpg" || 
                contentType == "image/png" || 
                contentType == "image/webp")
    }

    /**
     * Validate if the file size is within the allowed limit
     * @param file The file to validate
     * @param maxSizeBytes The maximum allowed size in bytes
     * @return True if the file size is within the limit, false otherwise
     */
    fun isValidFileSize(file: MultipartFile, maxSizeBytes: Long = 50 * 1024 * 1024): Boolean {
        return file.size <= maxSizeBytes
    }
}
