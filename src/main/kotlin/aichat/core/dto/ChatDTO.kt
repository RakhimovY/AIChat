package aichat.core.dto

import java.time.LocalDateTime

/**
 * Data Transfer Object for Chat
 * Contains only essential information without nested messages
 */
data class ChatDTO(
    val id: Long,
    val title: String?,
    val createdAt: LocalDateTime,
    val messageCount: Int
)