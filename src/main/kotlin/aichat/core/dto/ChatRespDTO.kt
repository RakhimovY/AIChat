package aichat.core.dto

import aichat.core.enums.ChatMessageRole
import java.time.LocalDateTime

data class ChatRespDTO(
    val id: Long,
    val role: ChatMessageRole,
    val content: String,
    val createdAt: LocalDateTime,
    val chatId: Long,
    val documentId: Long? = null,
    val documentName: String? = null,
    val documentUrl: String? = null
)
