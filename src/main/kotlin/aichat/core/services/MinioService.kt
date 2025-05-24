package aichat.core.services

import io.minio.*
import io.minio.errors.*
import io.minio.http.Method
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.security.InvalidKeyException
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.TimeUnit

@Service
class MinioService @Autowired constructor(
    private val minioClient: MinioClient,
    @Value("\${minio.bucket-name}") private val bucketName: String
) {
    private val logger = LoggerFactory.getLogger(MinioService::class.java)

    /**
     * Upload a file to MinIO
     * @param file The file to upload
     * @param objectName Optional custom object name, if not provided a UUID will be generated
     * @return The object name (key) in MinIO
     */
    fun uploadFile(file: MultipartFile, objectName: String? = null): String {
        try {
            val finalObjectName = objectName ?: "${UUID.randomUUID()}-${file.originalFilename}"
            
            // Upload the file to MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(finalObjectName)
                    .stream(file.inputStream, file.size, -1)
                    .contentType(file.contentType)
                    .build()
            )
            
            logger.info("File uploaded successfully: {}", finalObjectName)
            return finalObjectName
        } catch (e: Exception) {
            logger.error("Error uploading file to MinIO: {}", e.message, e)
            throw RuntimeException("Could not upload file to MinIO", e)
        }
    }

    /**
     * Upload byte array to MinIO
     * @param bytes The byte array to upload
     * @param objectName The object name (key) in MinIO
     * @param contentType The content type of the file
     * @return The object name (key) in MinIO
     */
    fun uploadBytes(bytes: ByteArray, objectName: String, contentType: String): String {
        try {
            val inputStream = ByteArrayInputStream(bytes)
            
            // Upload the byte array to MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .stream(inputStream, bytes.size.toLong(), -1)
                    .contentType(contentType)
                    .build()
            )
            
            logger.info("Bytes uploaded successfully: {}", objectName)
            return objectName
        } catch (e: Exception) {
            logger.error("Error uploading bytes to MinIO: {}", e.message, e)
            throw RuntimeException("Could not upload bytes to MinIO", e)
        }
    }

    /**
     * Download a file from MinIO
     * @param objectName The object name (key) in MinIO
     * @return The file as an InputStream
     */
    fun downloadFile(objectName: String): InputStream {
        try {
            // Get the object from MinIO
            val response = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .build()
            )
            
            logger.info("File downloaded successfully: {}", objectName)
            return response
        } catch (e: Exception) {
            logger.error("Error downloading file from MinIO: {}", e.message, e)
            throw RuntimeException("Could not download file from MinIO", e)
        }
    }

    /**
     * Get a presigned URL for a file in MinIO
     * @param objectName The object name (key) in MinIO
     * @param expiryTime The expiry time in seconds
     * @return The presigned URL
     */
    fun getPresignedUrl(objectName: String, expiryTime: Int = 7200): String {
        try {
            // Generate a presigned URL
            val url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucketName)
                    .`object`(objectName)
                    .expiry(expiryTime, TimeUnit.SECONDS)
                    .build()
            )
            
            logger.info("Presigned URL generated successfully: {}", url)
            return url
        } catch (e: Exception) {
            logger.error("Error generating presigned URL: {}", e.message, e)
            throw RuntimeException("Could not generate presigned URL", e)
        }
    }

    /**
     * Delete a file from MinIO
     * @param objectName The object name (key) in MinIO
     */
    fun deleteFile(objectName: String) {
        try {
            // Remove the object from MinIO
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .build()
            )
            
            logger.info("File deleted successfully: {}", objectName)
        } catch (e: Exception) {
            logger.error("Error deleting file from MinIO: {}", e.message, e)
            throw RuntimeException("Could not delete file from MinIO", e)
        }
    }

    /**
     * Check if a file exists in MinIO
     * @param objectName The object name (key) in MinIO
     * @return True if the file exists, false otherwise
     */
    fun fileExists(objectName: String): Boolean {
        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .`object`(objectName)
                    .build()
            )
            return true
        } catch (e: Exception) {
            return false
        }
    }
}