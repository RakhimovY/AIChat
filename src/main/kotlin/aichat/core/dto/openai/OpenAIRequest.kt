package aichat.core.dto.openai

/**
 * DTO representing a request to the OpenAI Chat Completions API
 */
data class OpenAIRequest(
    val model: String,
    val max_tokens: Int,
    val messages: List<Message>
)

/**
 * DTO representing a message in the OpenAI Chat Completions API
 */
data class Message(
    val role: String,
    val content: Any // Can be either a String or a List<Content>
)

/**
 * Base interface for content types
 */
interface Content {
    val type: String
}

/**
 * DTO representing text content
 */
data class TextContent(
    override val type: String = "text",
    val text: String
) : Content

/**
 * DTO representing image content
 */
data class ImageContent(
    override val type: String = "image_url",
    val image_url: ImageUrl
) : Content

/**
 * DTO representing an image URL
 */
data class ImageUrl(
    val url: String
)