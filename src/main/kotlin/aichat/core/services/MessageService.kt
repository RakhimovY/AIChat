package aichat.core.services

import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.enums.ChatMessageRole
import aichat.core.localization.Translations
import aichat.core.models.Chat
import aichat.core.models.Document
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.io.IOException
import kotlin.math.pow

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val chatService: ChatService,
    private val userService: UserService,
    private val documentService: DocumentService,
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

    // Cache for country name mapping to avoid repeated string operations
    private val countryNameCache = mutableMapOf<String, String>()

    // Static system prompt to avoid recreating it for each request
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

    // Pre-build language name mapping to avoid repeated string operations
    private val languageNameMap = mapOf(
        "ru" to "русском",
        "kk" to "казахском",
        "en" to "английском"
    )

    private val chatClient =
        chatClientBuilder.defaultAdvisors(MessageChatMemoryAdvisor(chatMemory)).build()

    fun createMessage(
        messageDTO: MessageDTO,
        userEmail: String,
    ): List<ChatRespDTO> {
        val chat =
            messageDTO.chatId?.let { chatService.getChatById(it) }
                ?: chatService.createChat(userService.getUserByEmail(userEmail), language = messageDTO.language)

        // Process document if present
        var document: Document? = null
        if (messageDTO.document != null && !messageDTO.document.isEmpty) {
            try {
                document = documentService.saveDocument(messageDTO.document, chat)
            } catch (e: IOException) {
                // Log error but continue with message creation
                val logger = org.slf4j.LoggerFactory.getLogger(MessageService::class.java)
                logger.error("Error saving document: ${e.message}", e)
            }
        }

        val message =
            Message(
                chat = chat,
                content = messageDTO.content,
                role = ChatMessageRole.user,
                document = document
            )
        chat.messages.add(message)
        messageRepository.save(message)

        // Update chat title based on first message if it's a new chat with default title
        if (chat.messages.size == 1 && (chat.title == "Новый чат" || chat.title == "Жаңа чат" || chat.title == "New Chat")) {
            // Generate title from user message (truncate if too long)
            val titleFromMessage = if (messageDTO.content.length > 50) {
                messageDTO.content.substring(0, 47) + "..."
            } else {
                messageDTO.content
            }
            chat.title = titleFromMessage
            chatRepository.save(chat)
        }

        // Extract document content for AI if present
        val documentContent = document?.let { documentService.extractTextContent(it) }

        askAI(messageDTO.content, chat, messageDTO.country, messageDTO.language, documentContent)

        // Generate document URLs for response
        return chat.messages.map { msg ->
            val msgDocument = msg.document
            val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()

            ChatRespDTO(
                id = msg.id,
                role = msg.role,
                content = msg.content,
                createdAt = msg.createdAt,
                chatId = msg.chat.id,
                documentId = msgDocument?.id,
                documentName = msgDocument?.name,
                documentUrl = msgDocument?.let { "$baseUrl/api/documents/${it.id}" }
            )
        }
    }

    // Function to map ISO country codes to full country names with caching
    private fun getCountryNameByCode(code: String): String {
        // Check if the country name is already in the cache
        return countryNameCache.getOrPut(code) {
            when (code) {
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
    }

    fun askAI(userMessage: String, chat: Chat, country: String? = null, language: String? = null, documentContent: String? = null) {
        val memoryId = chat.id.toString()
        val logger = org.slf4j.LoggerFactory.getLogger(MessageService::class.java)

        try {
            chatMemory.add(memoryId, listOf(UserMessage(userMessage)))

            // Create final prompt with country-specific and language-specific instructions
            // Use StringBuilder with initial capacity to reduce reallocations
            val promptBuilder = StringBuilder(systemPrompt.length + 1000) // Allocate enough space for the prompt, instructions, and document content

            // Start with system prompt
            promptBuilder.append(systemPrompt)
            promptBuilder.append("\n\n")

            // Add country-specific instructions if country is provided
            if (country != null) {
                // Map country code to full name if it's a code (using cached value)
                val countryName = getCountryNameByCode(country)

                promptBuilder.append("Пользователь находится в стране: ")
                    .append(countryName)
                    .append(".\n")
                    .append("Адаптируй свой ответ к правовой системе и законодательству этой страны.\n")
                    .append("Используй актуальные законы, нормативные акты и правовые документы этой страны.\n")
                    .append("Если цитируешь законы, конституцию или другие правовые документы, указывай их точные названия и номера статей.\n\n")
            }

            // Add language-specific instructions if language is provided
            if (language != null) {
                // Use pre-built language name mapping
                val languageName = languageNameMap[language] ?: "русском" // Default to Russian if unknown language

                promptBuilder.append("Пользователь предпочитает общение на ")
                    .append(languageName)
                    .append(" языке.\n")
                    .append("Пожалуйста, отвечай на ")
                    .append(languageName)
                    .append(" языке.\n\n")
            }

            // Add document content if provided
            if (documentContent != null) {
                promptBuilder.append("Пользователь предоставил следующий документ. Используй информацию из него для ответа на вопрос:\n\n")
                    .append("--- НАЧАЛО ДОКУМЕНТА ---\n")
                    .append(documentContent)
                    .append("\n--- КОНЕЦ ДОКУМЕНТА ---\n\n")
                    .append("Анализируй информацию из документа и используй ее для ответа на вопрос пользователя. ")
                    .append("Если документ содержит релевантную информацию, ссылайся на нее в своем ответе.\n\n")
            }

            // Add user message
            promptBuilder.append("Вот мой вопрос: ")
                .append(userMessage)

            val finalPrompt = promptBuilder.toString()

            logger.info("Sending prompt to AI for chat ${chat.id}, country: ${country ?: "not specified"}, language: ${language ?: "not specified"}")
            val startTime = System.currentTimeMillis()

            // Implement retry mechanism with exponential backoff
            val maxRetries = 3
            var retryCount = 0
            var aiContent: String? = null
            var lastException: Exception? = null

            while (retryCount < maxRetries && aiContent == null) {
                try {
                    // If this is a retry, log it
                    if (retryCount > 0) {
                        logger.info("Retry attempt $retryCount for chat ${chat.id}")
                    }

                    // Call AI service with timeout
                    aiContent = chatClient.prompt(finalPrompt).call().content()
                } catch (e: Exception) {
                    lastException = e
                    logger.error("Error calling AI service (attempt ${retryCount + 1}): ${e.message}", e)

                    // If we've reached max retries, break
                    if (retryCount >= maxRetries - 1) {
                        break
                    }

                    // Exponential backoff: wait longer between each retry
                    val backoffTime = (1000L * 2.0.pow(retryCount.toDouble())).toLong()
                    logger.info("Waiting ${backoffTime}ms before retry")
                    Thread.sleep(backoffTime)
                    retryCount++
                }
            }

            // If all retries failed, use error message
            if (aiContent == null) {
                aiContent = Translations.get("error", language)
                logger.error("All retry attempts failed for chat ${chat.id}", lastException)
            }

            val endTime = System.currentTimeMillis()
            logger.info("AI response received in ${endTime - startTime}ms for chat ${chat.id}")

            chatMemory.add(memoryId, listOf(AssistantMessage(aiContent)))

            val message =
                Message(
                    chat = chat,
                    content = aiContent,
                    role = ChatMessageRole.assistant
                )
            chat.messages.add(message)
            messageRepository.save(message)
        } catch (e: Exception) {
            logger.error("Unexpected error in askAI: ${e.message}", e)
            val errorMessage = Message(
                chat = chat,
                content = Translations.get("error", language),
                role = ChatMessageRole.assistant
            )
            chat.messages.add(errorMessage)
            messageRepository.save(errorMessage)
        }
    }

    fun getChatMessages(chatId: Long): List<ChatRespDTO> {
        val messages = messageRepository.findByChatId(chatId)
        val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()

        return messages.map { msg ->
            val document = msg.document
            ChatRespDTO(
                id = msg.id,
                role = msg.role,
                content = msg.content,
                createdAt = msg.createdAt,
                chatId = msg.chat.id,
                documentId = document?.id,
                documentName = document?.name,
                documentUrl = document?.let { "$baseUrl/api/documents/${it.id}" }
            )
        }
    }
}
