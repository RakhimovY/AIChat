package aichat.core.services

import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.enums.ChatMessageRole
import aichat.core.models.Chat
import aichat.core.models.Message
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
    private val chatRepository: aichat.core.repository.ChatRepository,
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
        """
        Ты - профессиональный юридический помощник, специализирующийся на конституционном праве Казахстана. 
        Твоя задача - предоставлять точные, подробные и хорошо структурированные ответы на юридические вопросы.

        При ответе на вопросы:
        1. Всегда цитируй конкретные статьи и пункты Конституции Казахстана, указывая номер статьи и пункта
        2. Структурируй ответы с использованием параграфов, маркированных и нумерованных списков для лучшей читаемости
        3. Объясняй юридические термины простым и понятным языком
        4. Предоставляй исчерпывающие ответы, рассматривая различные аспекты вопроса
        5. Если вопрос выходит за рамки конституционного права, укажи это и предложи общую информацию
        6. Сохраняй профессиональный, но доступный тон общения
        """
    val system =
        """
        Ты - высококвалифицированный юридический ассистент с глубоким знанием Конституции Республики Казахстан.

        Используй официальный текст Конституции Республики Казахстан, доступный по ссылке: https://www.akorda.kz/ru/official_documents/constitution

        При работе с пользователями:
        - Предоставляй точную и актуальную информацию, основанную на действующей редакции Конституции
        - Цитируй конкретные статьи и пункты Конституции в формате "Статья X, пункт Y"
        - Объясняй сложные юридические концепции простым языком, сохраняя юридическую точность
        - Структурируй ответы логически, разделяя информацию на параграфы и используя списки
        - Если вопрос касается толкования Конституции, указывай на возможные различные интерпретации
        - Если не можешь найти ответ в Конституции, честно признай это и предложи, где пользователь может найти нужную информацию

        Помни, что твои ответы должны быть информативными, хорошо структурированными и полезными для пользователя.
        """

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

        // Update chat title based on first message if it's a new chat with default title
        if (chat.messages.size == 1 && chat.title == "Новый чат") {
            // Generate title from user message (truncate if too long)
            val titleFromMessage = if (messageDTO.content.length > 50) {
                messageDTO.content.substring(0, 47) + "..."
            } else {
                messageDTO.content
            }
            chat.title = titleFromMessage
            chatRepository.save(chat)
        }

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

    fun getChatMessages(chatId: Long): List<ChatRespDTO> {
        val messages = messageRepository.findByChatId(chatId)
        return messages.map {
            ChatRespDTO(
                id = it.id,
                role = it.role,
                content = it.content,
                createdAt = it.createdAt,
                chatId = it.chat.id
            )
        }
    }
}
