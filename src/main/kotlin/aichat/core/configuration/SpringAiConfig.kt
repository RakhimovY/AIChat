package aichat.core.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter

@Configuration
class SpringAiConfig(private val objectMapper: ObjectMapper) {

    /**
     * Configures a RestTemplateBuilder with our custom ObjectMapper
     * This ensures that the ObjectMapper is configured to ignore unknown properties
     * during deserialization of OpenAI API responses
     * 
     * The @Primary annotation ensures this builder is used by Spring AI components
     */
    @Bean
    @Primary
    fun restTemplateBuilder(): RestTemplateBuilder {
        val messageConverter = MappingJackson2HttpMessageConverter(objectMapper)

        return RestTemplateBuilder()
            .additionalMessageConverters(messageConverter)
    }
}
