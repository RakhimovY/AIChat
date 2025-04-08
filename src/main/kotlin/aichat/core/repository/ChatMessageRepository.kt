package aichat.core.repository

import aichat.core.modles.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository

interface ChatMessageRepository : JpaRepository<ChatMessage, Long> {
    fun findTop10ByUserIdOrderByCreatedAtDesc(userId: Long): List<ChatMessage>
}