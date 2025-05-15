package aichat.web

import aichat.core.models.User
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
    ): ResponseEntity<User> {
        val user = userService.getUserByEmail(userDetails.username)
        
        // Update user fields
        user.email = request.email
        user.name = request.name
        user.country = request.country
        
        return userService.updateUserById(user.id, user)
    }
    
    @GetMapping("/profile")
    fun getProfile(
        @AuthenticationPrincipal userDetails: UserDetails
    ): ResponseEntity<User> {
        val user = userService.getUserByEmail(userDetails.username)
        return ResponseEntity.ok(user)
    }
}