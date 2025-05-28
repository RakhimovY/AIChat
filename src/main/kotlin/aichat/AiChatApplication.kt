package aichat
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.minio.MinioClient

@SpringBootApplication
class AiChatApplication {

    @Configuration
    @ConditionalOnProperty(name = ["spring.profiles.active"], havingValue = "test", matchIfMissing = false)
    class TestConfiguration {
        @Bean
        fun minioClient(): MinioClient {
            // Return a mock MinioClient for testing
            return MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("test", "test")
                .build()
        }
    }
}

fun main(args: Array<String>) {
    runApplication<AiChatApplication>(*args)
}
