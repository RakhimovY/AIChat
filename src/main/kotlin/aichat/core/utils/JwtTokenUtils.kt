package aichat.core.utils

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.stereotype.Component
import java.util.*

@Component
class JwtTokenUtils(
    @Value("\${jwt.secret}")
    private val secret: String,
    @Value("\${jwt.accessTokenExpiration}")
    private val accessTokenExpiration: String,
    @Value("\${jwt.refreshTokenExpiration}")
    private val refreshTokenExpiration: String,
) {

    fun generateToken(userDetails: UserDetails): String {
        val claims = Jwts.claims()
        claims["roles"] = userDetails.authorities.map { it.authority }
        val now = Date()
        val accessTokenExpirationDate = Date(now.time + accessTokenExpiration.toLong())
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.username)
            .setIssuedAt(now)
            .setExpiration(accessTokenExpirationDate)
            .signWith(SignatureAlgorithm.HS256, secret)
            .compact()
    }

    private fun getAllClaims(token: String) = Jwts.parser()
        .setSigningKey(secret)
        .parseClaimsJws(token)
        .body

    fun getUsername(token: String): String = getAllClaims(token).subject

    fun isExpired(token: String) = getAllClaims(token).expiration.before(Date())

    fun isValid(token: String, userDetails: UserDetails) =
        getUsername(token) == userDetails.username && !isExpired(token)

    fun getAuthentication(token: String, existingAuth: Authentication?, userDetails: UserDetails): UsernamePasswordAuthenticationToken {
        val claims = getAllClaims(token)
        val authorities = claims["roles"] as List<*>
        val grantedAuthorities = authorities.map { role -> SimpleGrantedAuthority(role as String) }.toList()
        return UsernamePasswordAuthenticationToken(userDetails, "", grantedAuthorities)
    }
}