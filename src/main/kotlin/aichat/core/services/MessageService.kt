package aichat.core.services

import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.enums.ChatMessageRole
import aichat.core.modles.Chat
import aichat.core.modles.Message
import aichat.core.repository.MessageRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val chatService: ChatService,
    private val userService: UserService,
    @Value("classpath:/prompts/law-ai.st") private val prompt: String,
    chatClientBuilder: ChatClient.Builder
) {
    private val chatClient = chatClientBuilder.build()

    fun createMessage(
        messageDTO: MessageDTO,
        userEmail: String,
    ): List<ChatRespDTO> {
        val chat = messageDTO.chatId?.let { chatService.getChatById(it) }
            ?: chatService.createChat(userService.getUserByEmail(userEmail))

        val message = Message(
            chat = chat,
            content = messageDTO.content,
            role = ChatMessageRole.user
        )
        chat.messages.add(message)
        messageRepository.save(message)
        askAI(messageDTO.content, chat)


        return chat.messages.map {
            ChatRespDTO(
                id = it.id,
                role = it.role,
                content = it.content,
                createdAt = it.createdAt,
                chatId = it.chat.id
            )
        }
    }

    fun askAI(userMessage: String, chat: Chat) {
        val aiResponse = chatClient.prompt()
            .system(prompt)
            .user(userMessage)
            .call()
            .content()

        val message = Message(
            chat = chat,
            content = aiResponse ?: "",
            role = ChatMessageRole.assistant
        )
        chat.messages.add(message)
        messageRepository.save(message)
    }
}