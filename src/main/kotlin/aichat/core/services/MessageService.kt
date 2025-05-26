package aichat.core.services

import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.enums.ChatMessageRole
import aichat.core.localization.Translations
import aichat.core.models.Chat
import aichat.core.models.Document
import aichat.core.models.Message
import aichat.core.repository.MessageRepository
import aichat.core.utils.RetryUtils
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemoryRepository
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.memory.jdbc.JdbcChatMemoryRepository
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.context.annotation.Lazy
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.servlet.support.ServletUriComponentsBuilder
import java.io.IOException

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    @Lazy private val chatService: ChatService,
    private val userService: UserService,
    private val documentService: DocumentService,
    private val jdbcTemplate: JdbcTemplate,
    private val chatRepository: aichat.core.repository.ChatRepository,
    private val countryService: CountryService,
    chatClientBuilder: ChatClient.Builder,
) {
    private val logger = LoggerFactory.getLogger(MessageService::class.java)

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
        val document = processDocument(messageDTO.document, chat)

        createAndSaveMessage(
            chat = chat,
            content = messageDTO.content,
            role = ChatMessageRole.user,
            document = document
        )

        // Update chat title based on first message if it's a new chat with default title
        updateChatTitleIfNeeded(chat, messageDTO.content)

        // Extract document content for AI if present
        val documentContent = document?.let { documentService.extractTextContent(it) }

        askAI(messageDTO.content, chat, messageDTO.country, messageDTO.language, documentContent)

        // Generate document URLs for response - fetch messages explicitly to avoid LazyInitializationException
        val messages = messageRepository.findByChatId(chat.id)
        return messages.map { mapMessageToDTO(it) }
    }

    /**
     * Build the AI prompt with country-specific and language-specific instructions
     */
    private fun buildPrompt(
        userMessage: String,
        country: String? = null,
        language: String? = null,
        documentContent: String? = null
    ): String {
        // Use StringBuilder with initial capacity to reduce reallocations
        val promptBuilder =
            StringBuilder(systemPrompt.length + 1000) // Allocate enough space for the prompt, instructions, and document content

        // Start with system prompt
        promptBuilder.append(systemPrompt)
        promptBuilder.append("\n\n")

        // Add country-specific instructions if country is provided
        if (country != null) {
            // Map country code to full name if it's a code (using CountryService)
            val countryName = countryService.getCountryNameByCode(country)

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

        return promptBuilder.toString()
    }

    /**
     * Ask the AI for a response to the user's message
     */
    fun askAI(
        userMessage: String,
        chat: Chat,
        country: String? = null,
        language: String? = null,
        documentContent: String? = null
    ) {
        val memoryId = chat.id.toString()
        val contextInfo = "for chat ${chat.id}"

        try {
            // Add user message to chat memory
            chatMemory.add(memoryId, listOf(UserMessage(userMessage)))

            // Build the prompt
            val finalPrompt = buildPrompt(userMessage, country, language, documentContent)

            logger.info("Sending prompt to AI $contextInfo, country: ${country ?: "not specified"}, language: ${language ?: "not specified"}")
            val startTime = System.currentTimeMillis()

            // Call AI service with retry mechanism
            val aiContent = try {
                RetryUtils.withRetryBlocking(
                    maxRetries = 3,
                    initialDelayMs = 1000,
                    logger = logger,
                    contextInfo = contextInfo
                ) {
                    // Call AI service
                    chatClient.prompt(finalPrompt).call().content()
                }
            } catch (e: Exception) {
                logger.error("All retry attempts failed $contextInfo", e)
                Translations.get("error", language)
            }

            val endTime = System.currentTimeMillis()
            logger.info("AI response received in ${endTime - startTime}ms $contextInfo")

            // Add assistant message to chat memory
            chatMemory.add(memoryId, listOf(AssistantMessage(aiContent)))

            // Create and save the message
            createAndSaveMessage(
                chat = chat,
                content = aiContent,
                role = ChatMessageRole.assistant
            )
        } catch (e: Exception) {
            handleAIError(chat, e, language)
        }
    }

    /**
     * Handle errors in AI processing
     */
    private fun handleAIError(chat: Chat, error: Exception, language: String?) {
        logger.error("Unexpected error in askAI: ${error.message}", error)

        // Create and save error message
        createAndSaveMessage(
            chat = chat,
            content = Translations.get("error", language),
            role = ChatMessageRole.assistant
        )
    }

    /**
     * Process document attachment if present
     */
    private fun processDocument(
        documentMultipart: org.springframework.web.multipart.MultipartFile?,
        chat: Chat
    ): Document? {
        if (documentMultipart == null || documentMultipart.isEmpty) {
            return null
        }

        return try {
            documentService.saveDocument(documentMultipart, chat)
        } catch (e: IOException) {
            // Log error but continue with message creation
            logger.error("Error saving document: ${e.message}", e)
            null
        }
    }

    /**
     * Update chat title based on the first message if it's a new chat with default title
     */
    private fun updateChatTitleIfNeeded(chat: Chat, content: String) {
        // List of default titles in different languages
        val defaultTitles = listOf("Новый чат", "Жаңа чат", "New Chat")

        // Get message count explicitly to avoid LazyInitializationException
        val messageCount = messageRepository.findByChatId(chat.id).size
        if (messageCount == 1 && defaultTitles.contains(chat.title)) {
            // Generate title from user message (truncate if too long)
            val titleFromMessage = if (content.length > 50) {
                content.substring(0, 47) + "..."
            } else {
                content
            }
            chat.title = titleFromMessage
            chatRepository.save(chat)
        }
    }

    /**
     * Create and save a message
     */
    private fun createAndSaveMessage(
        chat: Chat,
        content: String,
        role: ChatMessageRole,
        document: Document? = null
    ): Message {
        val message = Message(
            chat = chat,
            content = content,
            role = role,
            document = document
        )
        // Don't modify chat.messages directly to avoid LazyInitializationException
        // Just save the message with the chat reference and let Hibernate handle the relationship
        return messageRepository.save(message)
    }

    /**
     * Delete a message and its associated document if it exists
     */
    fun deleteMessage(message: Message) {
        messageRepository.delete(message)
    }

    /**
     * Delete messages and its associated document if it exists
     */
    fun deleteMessages(messages: List<Message>) {
        messages.forEach { message ->
            try {
                deleteMessage(message)
            } catch (e: Exception) {
                logger.error("Error deleting message ${message.id}", e)
            }
        }
    }


    /**
     * Map a Message entity to a ChatRespDTO
     */
    private fun mapMessageToDTO(message: Message): ChatRespDTO {
        val document = message.document
        val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()

        return ChatRespDTO(
            id = message.id,
            role = message.role,
            content = message.content,
            createdAt = message.createdAt,
            chatId = message.chat.id,
            documentId = document?.id,
            documentName = document?.name,
            documentUrl = document?.let { "$baseUrl/api/documents/${it.id}" }
        )
    }

    fun getChatMessages(chatId: Long): List<ChatRespDTO> {
        val messages = messageRepository.findByChatId(chatId)
        return messages.map { mapMessageToDTO(it) }
    }
}
