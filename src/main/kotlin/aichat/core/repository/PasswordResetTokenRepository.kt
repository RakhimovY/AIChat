package aichat.core.repository

import aichat.core.models.PasswordResetToken
import aichat.core.models.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {
    fun findByToken(token: String): Optional<PasswordResetToken>
    fun findByUser(user: User): List<PasswordResetToken>
    fun deleteByUser(user: User)
}