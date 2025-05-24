package aichat.core.configuration

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class MinioConfig {
    private val logger = LoggerFactory.getLogger(MinioConfig::class.java)

    @Value("\${minio.endpoint}")
    private lateinit var endpoint: String

    @Value("\${minio.access-key}")
    private lateinit var accessKey: String

    @Value("\${minio.secret-key}")
    private lateinit var secretKey: String

    @Value("\${minio.bucket-name}")
    private lateinit var bucketName: String

    @Bean
    fun minioClient(): MinioClient {
        logger.info("Initializing MinIO client with endpoint: {}", endpoint)
        
        val minioClient = MinioClient.builder()
            .endpoint(endpoint)
            .credentials(accessKey, secretKey)
            .build()

        // Check if the bucket exists, if not create it
        try {
            val bucketExists = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            )

            if (!bucketExists) {
                logger.info("Creating bucket: {}", bucketName)
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                )
                logger.info("Bucket created successfully: {}", bucketName)
            } else {
                logger.info("Bucket already exists: {}", bucketName)
            }
        } catch (e: Exception) {
            logger.error("Error checking/creating bucket: {}", e.message, e)
            throw e
        }

        return minioClient
    }

    @Bean
    fun bucketName(): String {
        return bucketName
    }
}