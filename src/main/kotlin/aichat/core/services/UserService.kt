package aichat.core.services

import aichat.core.dto.GoogleAuthRequest
import aichat.core.dto.PasswordResetResponse
import aichat.core.dto.UserDto
import aichat.core.exception.UserAlreadyExistException
import aichat.core.exception.UserNotFounded
import aichat.core.models.PasswordResetToken
import aichat.core.models.User
import aichat.core.repository.PasswordResetTokenRepository
import aichat.core.repository.UserRepository
import jakarta.transaction.Transactional
import org.springframework.http.ResponseEntity
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

typealias ApplicationUserDetails = org.springframework.security.core.userdetails.User

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val emailService: EmailService,
) : UserDetailsService {
    fun getUserByEmail(email: String): User {
        return userRepository.getUserByEmail(email).orElseThrow { UserNotFounded() }
    }

    fun findUserByParams(email: String): ResponseEntity<List<UserDto>> {
        val findUsersList =
            listOf(userRepository.getUserByEmail(email).orElseThrow { UserNotFounded() })
        return ResponseEntity.ok(findUsersList.map {
            UserDto(
                it.id,
                it.email,
                it.name,
                it.country,
                it.createdAt
            )
        })
    }

    fun creatNewUser(requestDto: aichat.core.dto.RegistrationRequest): User {
        userRepository.getUserByEmail(requestDto.email).ifPresent { throw UserAlreadyExistException() }


        return userRepository.save(
            User(
                0,
                requestDto.email,
                name = requestDto.name,
                passwordHash = passwordEncoder.encode(requestDto.password),
                country = requestDto.country
            )
        )
    }

    fun getUserById(userId: Long): ResponseEntity<UserDto> {
        val findUser = userRepository.getUserById(userId)
        if (findUser.isEmpty) {
            throw UserNotFounded()
        }
        return ResponseEntity.ok(
            UserDto(
                findUser.get().id,
                findUser.get().email,
                findUser.get().name,
                findUser.get().country,
                findUser.get().createdAt
            )
        )
    }

    @Transactional
    fun updateUserById(
        userId: Long,
        userRequest: User
    ): ResponseEntity<User> {
        val existingUser =
            userRepository.getUserById(userId).orElseThrow { UserNotFounded() }

        // Check if email is already used by another user
        userRepository.getUserByEmail(userRequest.email).ifPresent { user ->
            if (user.id != userId) {
                throw UserAlreadyExistException("Email is already used by another user")
            }
        }

        existingUser.email = userRequest.email
        existingUser.name = userRequest.name
        existingUser.country = userRequest.country
        existingUser.createdAt = userRequest.createdAt
        userRepository.save(existingUser)

        return ResponseEntity.ok(
            existingUser
        )
    }

    fun deleteUserById(userId: Long): ResponseEntity<User> {
        val user = userRepository.getUserById(userId).orElseThrow { UserNotFounded() }
        userRepository.deleteById(userId)
        return ResponseEntity.ok(
            user
        )
    }

    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.getUserByEmail(username)
        return user.get().mapToUserDetails()
    }

    private fun User.mapToUserDetails(): UserDetails {
        return ApplicationUserDetails.builder()
            .username(this.email)
            .password(this.passwordHash)
            .roles(this.role)
            .build()
    }

    /**
     * Find or create a user by Google ID
     * @param googleAuthRequest The Google authentication request
     * @return The user
     */
    fun findOrCreateGoogleUser(googleAuthRequest: GoogleAuthRequest): User {
        // Check if user with Google ID exists
        val userByGoogleId = userRepository.findByGoogleId(googleAuthRequest.googleId)
        if (userByGoogleId.isPresent) {
            return userByGoogleId.get()
        }

        // Check if user with email exists
        val userByEmail = userRepository.getUserByEmail(googleAuthRequest.email)
        if (userByEmail.isPresent) {
            // Update existing user with Google information
            val existingUser = userByEmail.get()
            existingUser.googleId = googleAuthRequest.googleId
            existingUser.picture = googleAuthRequest.picture
            existingUser.provider = "google"
            if (existingUser.name.isNullOrBlank() && !googleAuthRequest.name.isNullOrBlank()) {
                existingUser.name = googleAuthRequest.name
            }
            return userRepository.save(existingUser)
        }

        // Create new user with Google information
        val newUser = User(
            email = googleAuthRequest.email,
            name = googleAuthRequest.name,
            passwordHash = passwordEncoder.encode("google-auth-" + System.currentTimeMillis()), // Generate a random password for Google users
            googleId = googleAuthRequest.googleId,
            picture = googleAuthRequest.picture,
            provider = "google"
        )
        return userRepository.save(newUser)
    }

    /**
     * Create a password reset token for a user
     * @param email The email of the user
     * @return A response entity with a success message or error
     */
    fun createPasswordResetTokenForUser(email: String): ResponseEntity<PasswordResetResponse> {
        try {
            val user = getUserByEmail(email)

            // Check if the user is a Google user
            if (user.provider == "google") {
                return ResponseEntity.badRequest().body(
                    PasswordResetResponse(
                        message = "Пользователи, вошедшие через Google, не могут сбросить пароль. Пожалуйста, используйте вход через Google.",
                        success = false
                    )
                )
            }

            // Delete any existing tokens for this user
            passwordResetTokenRepository.findByUser(user).forEach { token ->
                passwordResetTokenRepository.delete(token)
            }

            // Create a new token
            val token = PasswordResetToken(user = user)
            val savedToken = passwordResetTokenRepository.save(token)

            // Send password reset email
            val emailSent = emailService.sendPasswordResetEmail(email, savedToken.token)

            return if (emailSent) {
                ResponseEntity.ok(
                    PasswordResetResponse(
                        message = "Инструкции по сбросу пароля отправлены на ваш email.",
                        success = true
                    )
                )
            } else {
                ResponseEntity.status(500).body(
                    PasswordResetResponse(
                        message = "Не удалось отправить email. Пожалуйста, попробуйте позже.",
                        success = false
                    )
                )
            }
        } catch (e: UserNotFounded) {
            // Don't reveal that the user doesn't exist for security reasons
            return ResponseEntity.ok(
                PasswordResetResponse(
                    message = "Если указанный email зарегистрирован, инструкции по сбросу пароля будут отправлены.",
                    success = true
                )
            )
        } catch (e: Exception) {
            println("Error creating password reset token: ${e.message}")
            e.printStackTrace()
            return ResponseEntity.badRequest().body(
                PasswordResetResponse(
                    message = "Произошла ошибка при обработке запроса: ${e.message}",
                    success = false
                )
            )
        }
    }

    /**
     * Verify a password reset token and reset the password
     * @param token The token to verify
     * @param newPassword The new password to set
     * @return A response entity with a success message or error
     */
    fun resetPasswordWithToken(token: String, newPassword: String): ResponseEntity<PasswordResetResponse> {
        try {
            val resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow { IllegalArgumentException("Invalid token") }

            // Check if the token is expired or used
            if (resetToken.isExpired() || resetToken.used) {
                return ResponseEntity.badRequest().body(
                    PasswordResetResponse(
                        message = "Токен недействителен или истек срок его действия.",
                        success = false
                    )
                )
            }

            // Get the user
            val user = resetToken.user

            // Check if the user is a Google user
            if (user.provider == "google") {
                return ResponseEntity.badRequest().body(
                    PasswordResetResponse(
                        message = "Пользователи, вошедшие через Google, не могут сбросить пароль. Пожалуйста, используйте вход через Google.",
                        success = false
                    )
                )
            }

            // Update the password
            user.passwordHash = passwordEncoder.encode(newPassword)
            userRepository.save(user)

            // Mark the token as used
            resetToken.used = true
            passwordResetTokenRepository.save(resetToken)

            return ResponseEntity.ok(
                PasswordResetResponse(
                    message = "Пароль успешно изменен.",
                    success = true
                )
            )
        } catch (e: IllegalArgumentException) {
            return ResponseEntity.badRequest().body(
                PasswordResetResponse(
                    message = "Недействительный токен.",
                    success = false
                )
            )
        } catch (e: Exception) {
            println("Error resetting password: ${e.message}")
            e.printStackTrace()
            return ResponseEntity.badRequest().body(
                PasswordResetResponse(
                    message = "Произошла ошибка при обработке запроса: ${e.message}",
                    success = false
                )
            )
        }
    }
}
