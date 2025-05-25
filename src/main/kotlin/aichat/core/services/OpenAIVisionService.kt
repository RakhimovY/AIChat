package aichat.core.services

import aichat.core.dto.openai.*
import aichat.core.models.Image
import aichat.core.utils.RetryUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.IOException
import java.io.InputStream
import java.util.*

@Service
class OpenAIVisionService(
    private val minioService: MinioService,
    private val restTemplate: RestTemplate,
    @Value("\${spring.ai.openai.api-key}") private val apiKey: String,
    @Value("\${spring.ai.openai.chat.options.model:gpt-4o}") private val model: String,
    @Value("\${spring.ai.openai.chat.options.max-tokens:1000}") private val maxTokens: Int
) {
    private val logger = LoggerFactory.getLogger(OpenAIVisionService::class.java)
    @Value("\${spring.ai.openai.base-url:https://api.openai.com}")
    private lateinit var baseUrl: String
    private val openaiApiUrl by lazy { "$baseUrl/v1/chat/completions" }

    /**
     * Analyze an image using OpenAI GPT-4-Vision
     * @param image The image to analyze
     * @param prompt The user's prompt/question about the image
     * @param systemPrompt The system prompt to provide context
     * @return The AI's response
     * @throws IllegalArgumentException if any of the input parameters are invalid
     * @throws RuntimeException if the API call fails after all retries
     */
    fun analyzeImage(image: Image, prompt: String, systemPrompt: String): String {
        // Validate input parameters
        if (prompt.isBlank()) {
            throw IllegalArgumentException("Prompt cannot be empty")
        }
        if (systemPrompt.isBlank()) {
            throw IllegalArgumentException("System prompt cannot be empty")
        }

        logger.info("Analyzing image: ${image.name}")

        var imageStream: InputStream? = null
        try {
            // Download the image from MinIO
            imageStream = minioService.downloadFile(image.objectName)

            // Convert the image to Base64
            val imageBytes = imageStream.readAllBytes()
            val base64Image = Base64.getEncoder().encodeToString(imageBytes)

            // Create the request body
            val requestBody = createRequestBody(base64Image, prompt, systemPrompt)

            // Set up headers
            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            headers.set("Authorization", "Bearer $apiKey")

            // Create the HTTP entity with the request object
            val entity = HttpEntity(requestBody, headers)

            // Use RetryUtils to handle retries with exponential backoff
            val content = RetryUtils.withRetryBlocking(
                operation = {
                    // Make the API call and deserialize the response directly to our DTO
                    val response = restTemplate.exchange(
                        openaiApiUrl,
                        HttpMethod.POST,
                        entity,
                        OpenAIResponse::class.java
                    )

                    // Get the response body or throw an exception if it's null
                    val responseBody = response.body ?: throw RuntimeException("Empty response from OpenAI API")

                    // Extract the content from the first choice's message
                    if (responseBody.choices.isEmpty()) {
                        throw RuntimeException("No choices returned in OpenAI API response")
                    }

                    val content = responseBody.choices[0].message.content
                    logger.info("Image analysis completed successfully")
                    content
                },
                logger = logger,
                operationName = "OpenAI Vision API call",
                contextInfo = "image ${image.id}"
            )

            return content
        } catch (e: Exception) {
            logger.error("Error during image analysis: ${e.message}", e)
            throw e
        } finally {
            // Close the image stream to prevent resource leakage
            try {
                imageStream?.close()
            } catch (e: IOException) {
                logger.warn("Failed to close image stream: ${e.message}", e)
            }
        }
    }

    /**
     * Create the request object for the OpenAI API using DTO classes
     */
    private fun createRequestBody(base64Image: String, prompt: String, systemPrompt: String): OpenAIRequest {
        // Create system message
        val systemMessage = Message(
            role = "system",
            content = systemPrompt
        )

        // Create user message with text and image content
        val textContent = TextContent(text = prompt)
        val imageContent = ImageContent(
            image_url = ImageUrl(url = "data:image/jpeg;base64,$base64Image")
        )

        val userMessage = Message(
            role = "user",
            content = listOf(textContent, imageContent)
        )

        // Create and return the complete request
        return OpenAIRequest(
            model = model,
            max_tokens = maxTokens,
            messages = listOf(systemMessage, userMessage)
        )
    }
}
