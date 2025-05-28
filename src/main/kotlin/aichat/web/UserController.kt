package aichat.web

import aichat.core.dto.UserDto
import aichat.core.services.UserService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.web.bind.annotation.*

data class UpdateProfileRequest(
    val name: String?,
    val email: String,
    val country: String?
)

@RestController
@RequestMapping("api/user")
class UserController(
    private val userService: UserService
) {
    @PostMapping("/update-profile")
    fun updateProfile(
        @RequestBody request: UpdateProfileRequest,
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<UserDto> {
        val user = userService.getUserByEmail(userDetails.username)
        val updatedUser = userService.updateUserById(user.id, user).body!!

        return ResponseEntity.ok(
            UserDto(
                updatedUser.id,
                updatedUser.email,
                updatedUser.name,
                updatedUser.country,
                updatedUser.createdAt
            )
        )
    }

    @GetMapping("/profile")
    fun getProfile(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<UserDto> {
        val user = userService.getUserByEmail(userDetails.username)

        // Convert to DTO to avoid circular references
        return ResponseEntity.ok(
            UserDto(
                user.id,
                user.email,
                user.name,
                user.country,
                user.createdAt
            )
        )
    }
}
