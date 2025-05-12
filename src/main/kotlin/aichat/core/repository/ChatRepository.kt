package aichat.core.repository

import aichat.core.models.Chat
import org.springframework.data.jpa.repository.JpaRepository

interface ChatRepository : JpaRepository<Chat, Long> {
    fun findAllByClientId(clientId: Long): List<Chat>
}
