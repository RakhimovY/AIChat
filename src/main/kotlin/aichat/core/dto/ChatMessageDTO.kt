package aichat.core.dto

import aichat.core.enums.ChatMessageRole

data class ChatMessageDTO(
    val role: ChatMessageRole,
    val content: String
)