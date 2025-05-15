package aichat.web

import aichat.core.dto.GoogleAuthRequest
import aichat.core.dto.LoginRequest
import aichat.core.dto.LoginResponse
import aichat.core.dto.PasswordResetRequest
import aichat.core.dto.PasswordResetResponse
import aichat.core.dto.PasswordResetVerificationRequest
import aichat.core.dto.RegistrationRequest
import aichat.core.models.User
import aichat.core.services.AuthService
import aichat.core.services.UserService
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
    private val userService: UserService,
) {
    @PostMapping("/sign-up", "/register")

    fun registerNewUser(
        @RequestBody @Validated registrationRequest: RegistrationRequest?
    ): ResponseEntity<User> {
        return authService.registerNewUser(registrationRequest)
    }

    @PostMapping("/sign-in")
    fun signIn(@RequestBody loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        return authService.createAuthToken(loginRequest)
    }

    /**
     * Authenticate a user with Google credentials
     * @param googleAuthRequest The Google authentication request
     * @return A response entity with the login response
     */
    @PostMapping("/google")
    fun googleAuth(@RequestBody googleAuthRequest: GoogleAuthRequest): ResponseEntity<LoginResponse> {
        return authService.authenticateWithGoogle(googleAuthRequest)
    }

    /**
     * Request a password reset
     * @param passwordResetRequest The password reset request
     * @return A response entity with a success message or error
     */
    @PostMapping("/forgot-password")
    fun forgotPassword(@RequestBody passwordResetRequest: PasswordResetRequest): ResponseEntity<PasswordResetResponse> {
        return userService.createPasswordResetTokenForUser(passwordResetRequest.email)
    }

    /**
     * Reset a password with a token
     * @param passwordResetVerificationRequest The password reset verification request
     * @return A response entity with a success message or error
     */
    @PostMapping("/reset-password")
    fun resetPassword(@RequestBody passwordResetVerificationRequest: PasswordResetVerificationRequest): ResponseEntity<PasswordResetResponse> {
        return userService.resetPasswordWithToken(
            passwordResetVerificationRequest.token,
            passwordResetVerificationRequest.newPassword
        )
    }
}
