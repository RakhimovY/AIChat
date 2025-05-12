package aichat.core.repository

import aichat.core.models.Message
import org.springframework.data.jpa.repository.JpaRepository

interface MessageRepository : JpaRepository<Message, Long> {
    fun findByChatId(chatId: Long): List<Message>
}
