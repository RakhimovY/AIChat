package aichat.core.services

import aichat.core.dto.GoogleAuthRequest
import aichat.core.dto.UserDto
import aichat.core.exception.UserAlreadyExistException
import aichat.core.exception.UserNotFounded
import aichat.core.models.User
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
    private val passwordEncoder: PasswordEncoder,
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
                requestDto.name,
                passwordEncoder.encode(requestDto.password),
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
            .roles("USER")
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
}
