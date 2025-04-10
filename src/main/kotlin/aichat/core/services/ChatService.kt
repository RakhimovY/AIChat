package aichat.core.services

import aichat.core.modles.Chat
import aichat.core.modles.User
import aichat.core.repository.ChatRepository
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val chatRepository: ChatRepository
) {

    fun createChat(title: String?, user: User): Chat {
        val chat = Chat(
            title = title,
            client = user
        )
        return chatRepository.save(chat)
    }

    fun getChatsByUserId(userId: Long): List<Chat> {
        return chatRepository.findAllByClientId(userId)
    }

    fun getChatById(id: Long): Chat? {
        return chatRepository.findById(id).orElse(null)
    }
}