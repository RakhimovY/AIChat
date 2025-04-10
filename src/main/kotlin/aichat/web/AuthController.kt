package aichat.web

import aichat.core.dto.LoginRequest
import aichat.core.dto.LoginResponse
import aichat.core.dto.RegistrationRequest
import aichat.core.modles.User
import aichat.core.services.AuthService
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("api/auth")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/sign-up")

    fun registerNewUser(
        @RequestBody @Validated registrationRequest: RegistrationRequest?
    ): ResponseEntity<User> {
        return authService.registerNewUser(registrationRequest)
    }

    @PostMapping("/sign-in")
    fun signIn(@RequestBody loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        return authService.createAuthToken(loginRequest)
    }
}