package aichat.core.dto.openai

/**
 * DTO representing a response from the OpenAI Chat Completions API
 */
data class OpenAIResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage
)

/**
 * DTO representing a choice in the OpenAI response
 */
data class Choice(
    val index: Int,
    val message: ResponseMessage,
    val finish_reason: String
)

/**
 * DTO representing a message in the OpenAI response
 */
data class ResponseMessage(
    val role: String,
    val content: String
)

/**
 * DTO representing usage information in the OpenAI response
 */
data class Usage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)