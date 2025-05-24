package aichat.core.dto

import org.springframework.web.multipart.MultipartFile

data class MessageDTO(
    val chatId: Long?,
    val content: String,
    val country: String? = null,
    val language: String? = null,
    val document: MultipartFile? = null
)
