package aichat.core.services

import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.enums.ChatMessageRole
import aichat.core.modles.Chat
import aichat.core.modles.Message
import aichat.core.repository.MessageRepository
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemoryRepository
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.memory.jdbc.JdbcChatMemoryRepository
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val chatService: ChatService,
    private val userService: UserService,
    private val jdbcTemplate: JdbcTemplate,
    chatClientBuilder: ChatClient.Builder,
) {
    private val chatMemoryRepository: ChatMemoryRepository =
        JdbcChatMemoryRepository.builder().jdbcTemplate(jdbcTemplate).build()

    private val chatMemory =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(10)
            .build()

    val prompt =
        "Ты - юридический помощник. Всегда вставляй ссылку или номер на статью Конституции который."
    val system =
        "Ты - юридический помощник, используй эту Конституцию который лежит по ссылке https://www.akorda.kz/ru/official_documents/constitution"

    private val chatClient =
        chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor(chatMemory)).build()

    fun createMessage(
        messageDTO: MessageDTO,
        userEmail: String,
    ): List<ChatRespDTO> {
        val chat =
            messageDTO.chatId?.let { chatService.getChatById(it) }
                ?: chatService.createChat(userService.getUserByEmail(userEmail))

        val message =
            Message(
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

        val aiContent =
            chatClient.prompt(prompt).system(system).user(userMessage).call().content()

        chatMemory.add(memoryId, listOf(AssistantMessage(aiContent ?: "")))

        val message =
            Message(
                chat = chat,
                content = aiContent ?: "",
                role = ChatMessageRole.assistant
            )
        chat.messages.add(message)
        messageRepository.save(message)
    }
}
