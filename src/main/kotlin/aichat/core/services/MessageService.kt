package aichat.core.services

import aichat.core.dto.MessageDTO
import aichat.core.enums.ChatMessageRole
import aichat.core.modles.Message
import aichat.core.repository.MessageRepository
import org.springframework.stereotype.Service

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val chatService: ChatService,
    private val userService: UserService
) {

    fun createMessage(
        messageDTO: MessageDTO,
        userEmail: String,
        role: ChatMessageRole = ChatMessageRole.user
    ): Message {
        if (messageDTO.chatId != null) {
            val chat = chatService.getChatById(messageDTO.chatId)
            val message = Message(
                chat = chat,
                content = messageDTO.content,
                role = role
            )
            chat.messages.add(message)
            return messageRepository.save(message)
        } else {
            val user = userService.getUserByEmail(userEmail)

            val chat = chatService.createChat(user)
            val message = Message(
                chat = chat,
                content = messageDTO.content,
                role = role
            )
            chat.messages.add(message)
            return messageRepository.save(message)
        }
    }

    fun getMessagesByChatId(chatId: Long): List<Message> {
        return messageRepository.findAllByChatId(chatId)
    }
}