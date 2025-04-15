package aichat.core.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent
import java.time.LocalDateTime


data class UserDto(
    @field: NotNull
    val id: Long,

    @field:NotBlank
    @field:Email
    val email: String,

    @field:PastOrPresent
    val createdAt: LocalDateTime
)