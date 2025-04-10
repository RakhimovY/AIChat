package aichat.core.services

import aichat.core.dto.UserDto
import aichat.core.exception.UserAlreadyExistException
import aichat.core.exception.UserNotFounded
import aichat.core.modles.User
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
//    fun getAllUsers(page: Int, size: Int): Page<UserDto> {
//        val sort = Sort.by(Sort.Direction.ASC, "id")
//        val pageable: Pageable = PageRequest.of(page, size, sort)
//        val result = userRepository.getAllWithPagination(pageable).map {
//            UserDto(
//                it.id,
//                it.email,
//                it.createdAt
//            )
//        }
//        return result
//    }

    fun getUserByEmail(email: String): ResponseEntity<User> {
        val findUser = userRepository.getUserByEmail(email)
        if (findUser.isEmpty) {
            throw UserNotFounded()
        }
        return ResponseEntity.ok(findUser.get())
    }

    fun findUserByParams(email: String): ResponseEntity<List<UserDto>> {
        val findUsersList =
            listOf(userRepository.getUserByEmail(email).orElseThrow { UserNotFounded() })
        return ResponseEntity.ok(findUsersList.map {
            UserDto(
                it.id,
                it.email,
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
}