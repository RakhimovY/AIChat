package aichat.core.dto

import org.springframework.security.core.GrantedAuthority

data class LoginResponse(
    val token: String,
    val privilege: Collection<String>,
    val name: String? = null,
    val country: String? = null
)
