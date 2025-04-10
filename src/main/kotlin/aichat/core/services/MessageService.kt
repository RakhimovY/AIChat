package aichat.core.services

import aichat.core.enums.ChatMessageRole
import aichat.core.modles.Chat
import aichat.core.modles.Message
import aichat.core.repository.MessageRepository
import org.springframework.stereotype.Service

@Service
class MessageService(
    private val messageRepository: MessageRepository
) {

    fun createMessage(chat: Chat, content: String, role: ChatMessageRole): Message {
        val message = Message(
            chat = chat,
            content = content,
            role = role
        )
        return messageRepository.save(message)
    }

    fun getMessagesByChatId(chatId: Long): List<Message> {
        return messageRepository.findAllByChatId(chatId)
    }
}