package aichat.core.services

import aichat.core.dto.GoogleAuthRequest
import aichat.core.dto.LoginRequest
import aichat.core.dto.LoginResponse
import aichat.core.dto.RegistrationRequest
import aichat.core.exception.UserAlreadyExistException
import aichat.core.exception.UserNotFounded
import aichat.core.models.User
import aichat.core.repository.UserRepository
import aichat.core.utils.JwtTokenUtils
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val authenticationManager: AuthenticationManager,
    private val jwtTokenUtils: JwtTokenUtils,
) {
    fun registerNewUser(requestDto: RegistrationRequest?): ResponseEntity<User> {
        if (requestDto == null) {
            throw IllegalArgumentException("Request body is null")
        }
        val userRepository = userRepository.getUserByEmail(requestDto.email)
        if (userRepository.isPresent) {
            throw UserAlreadyExistException()
        }

        val user = userService.creatNewUser(requestDto)

        return ResponseEntity.ok(
            user
        )
    }

    fun createAuthToken(loginRequest: LoginRequest): ResponseEntity<LoginResponse> {
        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(
                    loginRequest.email, loginRequest.password
                )
            )
        } catch (_: BadCredentialsException) {
            throw UserNotFounded()
        }
        val userDetails = userService.loadUserByUsername(loginRequest.email)
        val token = jwtTokenUtils.generateToken(userDetails)
        val user = userService.getUserByEmail(loginRequest.email)

        return ResponseEntity.ok(LoginResponse(
            token = token, 
            privilege = userDetails.authorities.map { it.authority },
            name = user.name,
            country = user.country
        ))
    }

    /**
     * Authenticate a user with Google credentials
     * @param googleAuthRequest The Google authentication request
     * @return A response entity with the login response
     */
    fun authenticateWithGoogle(googleAuthRequest: GoogleAuthRequest): ResponseEntity<LoginResponse> {
        // Find or create user with Google credentials
        val user = userService.findOrCreateGoogleUser(googleAuthRequest)

        // Generate JWT token
        val userDetails = userService.loadUserByUsername(user.email)
        val token = jwtTokenUtils.generateToken(userDetails)

        // Return login response
        return ResponseEntity.ok(LoginResponse(
            token = token,
            privilege = userDetails.authorities.map { it.authority },
            name = user.name
        ))
    }
}
