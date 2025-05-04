package aichat.core.services

import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.enums.ChatMessageRole
import aichat.core.modles.Chat
import aichat.core.modles.Message
import aichat.core.repository.MessageRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val chatService: ChatService,
    private val userService: UserService,
    private val chatMemory: ChatMemory,
    @Value("classpath:/prompts/law-ai.st") private val prompt: String,
    chatClientBuilder: ChatClient.Builder
) {
    private val chatClient = chatClientBuilder
        .defaultAdvisors(MessageChatMemoryAdvisor(chatMemory))
        .build()

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
        val memoryId = chat.id.toString()

        chatMemory.add(memoryId, listOf(UserMessage(userMessage)))

        val aiContent = chatClient
            .prompt()
            .system(prompt)
            .user(userMessage)
            .call()
            .content()

        chatMemory.add(memoryId, listOf(AssistantMessage(aiContent ?: "")))

        val message = Message(
            chat = chat,
            content = aiContent ?: "",
            role = ChatMessageRole.assistant
        )
        chat.messages.add(message)
        messageRepository.save(message)
    }


}