package aichat.core.services

import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.enums.ChatMessageRole
import aichat.core.localization.Translations
import aichat.core.models.Chat
import aichat.core.models.Document
import aichat.core.models.Image
import aichat.core.models.Message
import aichat.core.repository.MessageRepository
import aichat.core.utils.DTOMapper
import aichat.core.utils.PromptBuilder
import aichat.core.utils.RetryUtils
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.memory.ChatMemoryRepository
import org.springframework.ai.chat.memory.MessageWindowChatMemory
import org.springframework.ai.chat.memory.jdbc.JdbcChatMemoryRepository
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import java.io.IOException

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val chatService: ChatService,
    private val userService: UserService,
    private val documentService: DocumentService,
    private val imageService: ImageService,
    private val openAIVisionService: OpenAIVisionService,
    private val jdbcTemplate: JdbcTemplate,
    private val chatRepository: aichat.core.repository.ChatRepository,
    chatClientBuilder: ChatClient.Builder,
) {
    // Logger for this class
    private val logger = LoggerFactory.getLogger(MessageService::class.java)

    private val chatMemoryRepository: ChatMemoryRepository =
        JdbcChatMemoryRepository.builder().jdbcTemplate(jdbcTemplate).build()

    private val chatMemory =
        MessageWindowChatMemory.builder()
            .chatMemoryRepository(chatMemoryRepository)
            .maxMessages(15)
            .build()

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
                logger.error("Error saving document: ${e.message}", e)
            }
        }

        // Process image if present
        var image: Image? = null
        if (messageDTO.image != null && !messageDTO.image.isEmpty) {
            try {
                image = imageService.saveImage(messageDTO.image, chat)
            } catch (e: IOException) {
                // Log error but continue with message creation
                logger.error("Error saving image: ${e.message}", e)
            }
        }

        // Create and save user message
        val userMessage = Message(
            chat = chat,
            content = messageDTO.content,
            role = ChatMessageRole.user,
            document = document,
            image = image
        )
        chat.messages.add(userMessage)
        messageRepository.save(userMessage)

        // Update chat title based on first message if it's a new chat with default title
        if (chat.messages.size == 1 && (chat.title == "Новый чат" || chat.title == "Жаңа чат" || chat.title == "New Chat")) {
            // Generate title from user message (truncate if too long)
            val titleFromMessage = if (messageDTO.content.length > 50) {
                messageDTO.content.substring(0, 47) + "..."
            } else {
                messageDTO.content
            }
            chat.title = titleFromMessage
        }

        // Extract document content and image info for AI
        val documentContent = document?.let { documentService.extractTextContent(it) }
        val imageInfo = image?.let { "Image: ${it.name} (${it.contentType})" }

        // Ask AI for a response
        askAI(messageDTO.content, chat, messageDTO.country, messageDTO.language, documentContent, imageInfo)

        // Get the last two messages (user message and AI response)
        val newMessages = messageRepository.findByChatId(chat.id).takeLast(2)

        // Save the updated chat
        chatRepository.save(chat)

        // Map messages to DTOs
        return DTOMapper.mapMessagesToDTO(newMessages)
    }


    /**
     * Processes an image using GPT-4-Vision
     * @return true if image was successfully processed, false otherwise
     */
    private fun processImageWithVision(
        userMessage: String,
        chat: Chat,
        image: Image,
        country: String?,
        language: String?,
        memoryId: String
    ): Boolean {
        logger.info("Message contains an image. Using GPT-4-Vision for analysis.")

        try {
            // Get system prompt from PromptBuilder
            val visionSystemPrompt = PromptBuilder.buildSystemPrompt(country, language)

            // Call Vision API
            val aiContent = openAIVisionService.analyzeImage(
                image = image,
                prompt = userMessage,
                systemPrompt = visionSystemPrompt
            )

            // Add response to chat memory
            chatMemory.add(memoryId, listOf(AssistantMessage(aiContent)))

            // Create and save assistant message
            val message = Message(
                chat = chat,
                content = aiContent,
                role = ChatMessageRole.assistant
            )
            chat.messages.add(message)
            messageRepository.save(message)

            return true
        } catch (e: Exception) {
            logger.error("Error analyzing image with GPT-4-Vision: ${e.message}", e)
            logger.info("Falling back to standard text-based AI")
            return false
        }
    }

    /**
     * Creates and saves an assistant message
     */
    private fun saveAssistantMessage(
        chat: Chat,
        content: String,
        role: ChatMessageRole = ChatMessageRole.assistant
    ): Message {
        val message = Message(
            chat = chat,
            content = content,
            role = role
        )
        chat.messages.add(message)
        messageRepository.save(message)
        return message
    }

    /**
     * Asks the AI for a response to a user message
     */
    fun askAI(
        userMessage: String,
        chat: Chat,
        country: String? = null,
        language: String? = null,
        documentContent: String? = null,
        imageInfo: String? = null
    ) {
        val memoryId = chat.id.toString()

        try {
            // Add user message to chat memory
            chatMemory.add(memoryId, listOf(UserMessage(userMessage)))

            // Check if message has an image
            val latestMessage = chat.messages.lastOrNull { it.role == ChatMessageRole.user }
            val image = latestMessage?.image

            // If image exists, try to process it with Vision API
            if (image != null && processImageWithVision(userMessage, chat, image, country, language, memoryId)) {
                // Image was successfully processed, return early
                return
            }

            // Build prompt using PromptBuilder
            val finalPrompt = PromptBuilder.buildCompletePrompt(
                country = country,
                language = language,
                documentContent = documentContent,
                imageInfo = imageInfo,
                userMessage = userMessage
            )

            logger.info("Sending prompt to AI for chat ${chat.id}, country: ${country ?: "not specified"}, language: ${language ?: "not specified"}")
            val startTime = System.currentTimeMillis()

            // Use RetryUtils to handle retries with exponential backoff
            val aiContent = try {
                RetryUtils.withRetryBlocking(
                    operation = { chatClient.prompt(finalPrompt).call().content() },
                    logger = logger,
                    operationName = "AI service call",
                    contextInfo = "chat ${chat.id}"
                )
            } catch (e: Exception) {
                logger.error("All retry attempts failed for chat ${chat.id}", e)
                Translations.get("error", language)
            }

            val endTime = System.currentTimeMillis()
            logger.info("AI response received in ${endTime - startTime}ms for chat ${chat.id}")

            // Add response to chat memory
            chatMemory.add(memoryId, listOf(AssistantMessage(aiContent)))

            // Create and save assistant message
            saveAssistantMessage(chat, aiContent)

        } catch (e: Exception) {
            logger.error("Unexpected error in askAI: ${e.message}", e)

            // Create error message
            saveAssistantMessage(
                chat = chat,
                content = Translations.get("error", language)
            )
        }
    }

    /**
     * Gets all messages for a chat
     * @param chatId ID of the chat
     * @return List of message DTOs
     */
    fun getChatMessages(chatId: Long): List<ChatRespDTO> {
        val messages = messageRepository.findByChatId(chatId)
        return DTOMapper.mapMessagesToDTO(messages)
    }
}
