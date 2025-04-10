package aichat.core.repository

import aichat.core.modles.User
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    fun getUserByEmail(email: String): Optional<User>

    fun getUserById(userId: Long): Optional<User>
}