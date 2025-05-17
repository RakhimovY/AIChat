package aichat.core.repository

import aichat.core.models.Document
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface DocumentRepository : JpaRepository<Document, Long> {
    fun findByChatId(chatId: Long): List<Document>

    // New method that uses the chat property directly
    fun findByChat_Id(chatId: Long): List<Document>
}
