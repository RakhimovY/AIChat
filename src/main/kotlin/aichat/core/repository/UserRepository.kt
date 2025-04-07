package aichat.core.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import aichat.core.modles.User
import java.util.*

interface UserRepository : JpaRepository<User, Long> {
    @Query("SELECT u FROM User u")
    fun getAllWithPagination(pageable: Pageable): Page<User>

    fun getUserByEmail(email: String): Optional<User>

    fun getUserById(userId: Long): Optional<User>
}