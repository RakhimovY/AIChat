package aichat.core.repository

import aichat.core.modles.Message
import org.springframework.data.jpa.repository.JpaRepository

interface MessageRepository : JpaRepository<Message, Long> {
    fun findAllByChatId(chatId: Long): List<Message>
}