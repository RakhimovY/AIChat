package aichat.core.exception

class UserAlreadyExistException(errorMessage: String = "User already exist") : Exception(errorMessage)
class UserNotFounded() : Exception("User not founded")
class ChatNotFounded() : Exception("Chat not founded")
class TokenExpiredException(message: String) : RuntimeException(message)
class TokenInvalidException(message: String) : RuntimeException(message)
