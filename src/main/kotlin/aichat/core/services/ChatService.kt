package aichat.core.services

import aichat.core.dto.ChatDTO
import aichat.core.exception.ChatNotFounded
import aichat.core.localization.Translations
import aichat.core.models.Chat
import aichat.core.models.Message
import aichat.core.models.User
import aichat.core.repository.ChatRepository
import org.springframework.stereotype.Service

@Service
class ChatService(
    private val chatRepository: ChatRepository,
    private val userService: UserService,
    private val imageRepository: aichat.core.repository.ImageRepository,
    private val messageRepository: aichat.core.repository.MessageRepository
) {

    fun createChat(user: User, title: String? = null, language: String? = null): Chat {
        val chatTitle = title ?: Translations.get("defaultTitle", language)
        val chat = Chat(
            title = chatTitle, client = user
        )
        return chatRepository.save(chat)
    }

    fun getChatsByUserId(userId: Long): List<Chat> {
        return chatRepository.findAllByClientId(userId)
    }

    fun getChatById(id: Long): Chat {
        return chatRepository.findById(id).orElseThrow { ChatNotFounded() }
    }

    fun deleteChat(id: Long) {
        val chat = getChatById(id)

        // First, find all messages that reference images from this chat
        val messages = messageRepository.findByChatId(id)
        val messagesToUpdate = messages.filter { it.image != null }

        // Set image references to null and save the messages
        if (messagesToUpdate.isNotEmpty()) {
            // We need to create new Message objects with null image references
            // since the image property is immutable (val)
            // Make sure the chat is saved before saving messages that reference it
            chatRepository.save(chat)

            messagesToUpdate.forEach { message ->
                val updatedMessage = Message(
                    id = message.id,
                    role = message.role,
                    content = message.content,
                    createdAt = message.createdAt,
                    chat = message.chat,
                    document = message.document,
                    image = null  // Set image reference to null
                )
                messageRepository.save<Message>(updatedMessage)
            }
        }

        // Now find and delete all images associated with this chat
        val images = imageRepository.findByChat_Id(id)
        if (images.isNotEmpty()) {
            imageRepository.deleteAll(images)
        }

        // Finally delete the chat (which will cascade delete messages)
        chatRepository.delete(chat)
    }

    fun getUserChats(userEmail: String): List<Chat> {
        val user = userService.getUserByEmail(userEmail)
        return getChatsByUserId(user.id)
    }

    /**
     * Get all chats for a user as DTOs with simplified structure
     * @param userEmail The email of the user
     * @return List of ChatDTO objects
     */
    fun getUserChatsAsDTO(userEmail: String): List<ChatDTO> {
        val user = userService.getUserByEmail(userEmail)
        val chats = getChatsByUserId(user.id)
        return chats.map { chat ->
            ChatDTO(
                id = chat.id,
                title = chat.title,
                createdAt = chat.createdAt,
                messageCount = chat.messages.size
            )
        }
    }
}
