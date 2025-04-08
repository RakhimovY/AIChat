package aichat.core.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PastOrPresent


data class UserDto(
    @field: NotNull
    val id: Long,

    @field:NotBlank
    @field:Email
    val email: String,

    @field:PastOrPresent
    val registrationDate: String
)