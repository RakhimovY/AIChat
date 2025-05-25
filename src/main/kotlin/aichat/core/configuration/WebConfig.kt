package aichat.core.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.servlet.MultipartConfigElement
import org.springframework.boot.web.servlet.MultipartConfigFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.unit.DataSize
import org.springframework.web.client.RestTemplate
import org.springframework.web.multipart.MultipartResolver
import org.springframework.web.multipart.support.StandardServletMultipartResolver

@Configuration
class WebConfig {

    @Bean
    fun restTemplate(objectMapper: ObjectMapper): RestTemplate {
        val restTemplate = RestTemplate()

        // Configure message converters to use our custom ObjectMapper
        restTemplate.messageConverters.forEach { converter ->
            if (converter is org.springframework.http.converter.json.MappingJackson2HttpMessageConverter) {
                converter.objectMapper = objectMapper
            }
        }

        return restTemplate
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
            .registerModule(KotlinModule.Builder().build())
            .registerModule(JavaTimeModule())
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    @Bean
    fun multipartResolver(): MultipartResolver {
        return StandardServletMultipartResolver()
    }

    @Bean
    fun multipartConfigElement(): MultipartConfigElement {
        val factory = MultipartConfigFactory()
        factory.setMaxFileSize(DataSize.ofMegabytes(50))
        factory.setMaxRequestSize(DataSize.ofMegabytes(50))
        return factory.createMultipartConfig()
    }
}
