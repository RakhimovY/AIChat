package aichat.core.exception

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(value = [TokenExpiredException::class, TokenInvalidException::class])
    fun handleTokenExpiredException(ex: TokenExpiredException): ResponseEntity<String> {
        return ResponseEntity(ex.message, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(IllegalArgumentException::class, UserAlreadyExistException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<String> {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.message)
    }
}