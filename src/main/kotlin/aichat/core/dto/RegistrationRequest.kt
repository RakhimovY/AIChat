package aichat.core.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length


data class RegistrationRequest(
    @field: NotBlank @field: Length(
        min = 12,
        message = "Invalid password. Password must be at least 12 characters long."
    ) @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
        message = "Invalid password. Password must contain at least one lowercase letter, one uppercase letter, one digit, and one special character."
    ) val password: String,

    @field:NotBlank @field:Email val email: String,

    val name: String? = null,

    val country: String? = null
)
