package aichat.core.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.openai.OpenAiChatClient
import org.springframework.ai.openai.OpenAiChatOptions
import org.springframework.ai.openai.api.OpenAiApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestTemplate

@Configuration
class OpenAIConfig {
    // Logger for this class
    private val logger = LoggerFactory.getLogger(OpenAIConfig::class.java)

    @Value("\${spring.ai.openai.api-key}")
    private lateinit var apiKey: String

    @Value("\${spring.ai.openai.base-url:https://api.openai.com}")
    private lateinit var baseUrl: String

    @Value("\${spring.ai.openai.chat.options.model:gpt-4o-mini}")
    private lateinit var model: String

    @Value("\${spring.ai.openai.chat.options.temperature:0.7}")
    private var temperature: Double = 0.7

    @Value("\${spring.ai.openai.chat.options.max-tokens:1000}")
    private var maxTokens: Int = 1000

    @Bean
    @Primary
    fun openAiApi(restTemplate: RestTemplate, objectMapper: ObjectMapper): OpenAiApi {
        // Configure the OpenAiApi with the API key, base URL, RestTemplate, and ObjectMapper
        val api = OpenAiApi(apiKey, baseUrl, restTemplate, objectMapper)

        // Log that the OpenAiApi has been configured
        logger.info("Configured OpenAiApi with base URL: $baseUrl and model: $model")

        return api
    }

    @Bean
    @Primary
    fun openAiChatClient(openAiApi: OpenAiApi): OpenAiChatClient {
        // Configure the OpenAiChatOptions with the model, temperature, and max tokens
        val options = OpenAiChatOptions.builder()
            .withModel(model)
            .withTemperature(temperature)
            .withMaxTokens(maxTokens)
            .build()

        // Create the OpenAiChatClient with the OpenAiApi and options
        val client = OpenAiChatClient(openAiApi)
            .withDefaultOptions(options)

        // Log that the OpenAiChatClient has been configured
        logger.info("Configured OpenAiChatClient with model: $model, temperature: $temperature, maxTokens: $maxTokens")

        return client
    }
}
