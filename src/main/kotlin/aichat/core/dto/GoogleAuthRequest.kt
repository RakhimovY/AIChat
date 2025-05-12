package aichat.core.dto

data class GoogleAuthRequest(
    val token: String,
    val email: String,
    val name: String? = null,
    val picture: String? = null,
    val googleId: String
)