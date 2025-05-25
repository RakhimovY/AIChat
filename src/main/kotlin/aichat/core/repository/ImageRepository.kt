package aichat.core.repository

import aichat.core.models.Image
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImageRepository : JpaRepository<Image, Long> {
    fun findByChat_Id(chatId: Long): List<Image>
}