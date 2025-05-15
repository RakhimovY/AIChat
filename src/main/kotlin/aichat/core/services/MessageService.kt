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

    // Configure chat memory with a window of messages to provide context for the AI
    // Increasing maxMessages provides more context but may slow down responses
    // and increase token usage. 15 is a good balance between context and performance.
    private val chatMemory =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(15)
            .build()

    val systemPrompt =
        """
        Ты - высококвалифицированный юридический ассистент с глубоким знанием правовых систем разных стран.
        Твоя задача - предоставлять точные, подробные и хорошо структурированные ответы на юридические вопросы.

        Содержание и структура ответов:
        1. Предоставляй точную и актуальную информацию, основанную на действующем законодательстве страны пользователя
        2. Всегда цитируй конкретные статьи и пункты законов в формате "Статья X, пункт Y" с правильной нумерацией
        3. Объясняй юридические термины простым и понятным языком, сохраняя юридическую точность
        4. Предоставляй исчерпывающие ответы, рассматривая различные аспекты вопроса
        5. Если вопрос касается толкования законов, указывай на возможные различные интерпретации
        6. Если вопрос выходит за рамки правовой тематики, укажи это и предложи общую информацию
        7. Если не можешь найти ответ в законодательстве, честно признай это и предложи, где пользователь может найти нужную информацию
        8. Сохраняй профессиональный, но доступный тон общения

        Форматирование для лучшей читаемости:
        1. Структурируй ответы логически, разделяя параграфы пустой строкой
        2. Выделяй важные моменты **двойными звездочками** вокруг текста
        3. Используй списки для перечисления пунктов:
           - Дефисы для ненумерованных списков
           - Цифры с точкой для нумерованных списков

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

        askAI(messageDTO.content, chat, messageDTO.country)

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

    // Function to map ISO country codes to full country names
    private fun getCountryNameByCode(code: String): String {
        return when (code) {
            "RU" -> "Россия"
            "KZ" -> "Казахстан"
            "BY" -> "Беларусь"
            "UA" -> "Украина"
            "UZ" -> "Узбекистан"
            "KG" -> "Кыргызстан"
            "TJ" -> "Таджикистан"
            "TM" -> "Туркменистан"
            "AZ" -> "Азербайджан"
            "AM" -> "Армения"
            "GE" -> "Грузия"
            "MD" -> "Молдова"
            "US" -> "США"
            "GB" -> "Великобритания"
            "DE" -> "Германия"
            "FR" -> "Франция"
            "IT" -> "Италия"
            "ES" -> "Испания"
            "CN" -> "Китай"
            "JP" -> "Япония"
            "IN" -> "Индия"
            "BR" -> "Бразилия"
            "CA" -> "Канада"
            "AU" -> "Австралия"
            else -> code // Return the code itself if not found in the mapping
        }
    }

    fun askAI(userMessage: String, chat: Chat, country: String? = null) {
        val memoryId = chat.id.toString()
        val logger = org.slf4j.LoggerFactory.getLogger(MessageService::class.java)

        try {
            chatMemory.add(memoryId, listOf(UserMessage(userMessage)))

            // Create final prompt with country-specific instructions if country is provided
            val finalPrompt = if (country != null) {
                // Map country code to full name if it's a code
                val countryName = getCountryNameByCode(country)

                """
                $systemPrompt

                Пользователь находится в стране: $countryName.
                Адаптируй свой ответ к правовой системе и законодательству этой страны.
                Используй актуальные законы, нормативные акты и правовые документы этой страны.
                Если цитируешь законы, конституцию или другие правовые документы, указывай их точные названия и номера статей.

                Вот мой вопрос: $userMessage
                """
            } else {
                """
                $systemPrompt

                Вот мой вопрос: $userMessage
                """
            }

            logger.info("Sending prompt to AI for chat ${chat.id}, country: ${country ?: "not specified"}")
            val startTime = System.currentTimeMillis()

            val aiContent = try {
                chatClient.prompt(finalPrompt).call().content()
            } catch (e: Exception) {
                logger.error("Error calling AI service: ${e.message}", e)
                "Извините, произошла ошибка при обработке вашего запроса. Пожалуйста, попробуйте еще раз позже."
            }

            val endTime = System.currentTimeMillis()
            logger.info("AI response received in ${endTime - startTime}ms for chat ${chat.id}")

            chatMemory.add(memoryId, listOf(AssistantMessage(aiContent ?: "")))

            val message =
                Message(
                    chat = chat,
                    content = aiContent ?: "",
                    role = ChatMessageRole.assistant
                )
            chat.messages.add(message)
            messageRepository.save(message)
        } catch (e: Exception) {
            logger.error("Unexpected error in askAI: ${e.message}", e)
            val errorMessage = Message(
                chat = chat,
                content = "Произошла системная ошибка. Пожалуйста, попробуйте еще раз позже.",
                role = ChatMessageRole.assistant
            )
            chat.messages.add(errorMessage)
            messageRepository.save(errorMessage)
        }
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
