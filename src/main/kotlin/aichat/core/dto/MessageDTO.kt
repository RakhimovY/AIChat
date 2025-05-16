package aichat.core.dto

data class MessageDTO(
    val chatId: Long?,
    val content: String,
    val country: String? = null,
    val language: String? = null,
)
