package aichat.core.dto

data class PasswordResetVerificationRequest(
    val token: String,
    val newPassword: String
)