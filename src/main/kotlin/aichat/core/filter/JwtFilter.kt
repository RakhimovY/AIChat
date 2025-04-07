package aichat.core.filter

import io.jsonwebtoken.ExpiredJwtException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import aichat.core.services.UserService
import aichat.core.utils.JwtTokenUtils
import java.security.SignatureException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Component
class JwtFilter(
    private val userService: UserService,
    private val jwtTokenUtil: JwtTokenUtils,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")
        var username: String? = null
        var jwtToken: String? = null

        if(authHeader != null && authHeader.startsWith("Bearer ")) {
            jwtToken = authHeader.substring(7)
            try {
                username = jwtTokenUtil.getUsername(jwtToken)
            } catch (_: ExpiredJwtException) {
                logger.debug("JWT token is expired")
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token is expired")
                return
            } catch (_: SignatureException) {
                logger.debug("JWT token is invalid")
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "JWT token is invalid")
                return
            }
        }

        if (username != null && SecurityContextHolder.getContext().authentication == null) {
            val userDetails: UserDetails = userService.loadUserByUsername(username)

            // If token is valid configure Spring Security to manually set authentication
            if (jwtTokenUtil.isValid(jwtToken!!, userDetails)) {
                val authenticationToken = jwtTokenUtil.getAuthentication(jwtToken, SecurityContextHolder.getContext().authentication, userDetails)
                authenticationToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                SecurityContextHolder.getContext().authentication = authenticationToken

                // Check if token needs to be refreshed
                if (jwtTokenUtil.isExpired(jwtToken)) {
                    val newToken = jwtTokenUtil.generateToken(userDetails)
                    response.setHeader("Authorization", "Bearer $newToken")
                }
            }
        }
        filterChain.doFilter(request, response)
    }
}