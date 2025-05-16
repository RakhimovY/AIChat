package aichat.core.services

import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.enums.ChatMessageRole
import aichat.core.localization.Translations
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
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.Executors

// Interface for the response chunk type from Spring AI
interface ChatResponseChunk {
    fun content(): String
}

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
                ?: chatService.createChat(userService.getUserByEmail(userEmail), language = messageDTO.language)

        val message =
            Message(
                chat = chat,
                content = messageDTO.content,
                role = ChatMessageRole.user
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

        askAI(messageDTO.content, chat, messageDTO.country, messageDTO.language)

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

    fun askAI(userMessage: String, chat: Chat, country: String? = null, language: String? = null) {
        val memoryId = chat.id.toString()
        val logger = org.slf4j.LoggerFactory.getLogger(MessageService::class.java)

        try {
            chatMemory.add(memoryId, listOf(UserMessage(userMessage)))

            // Create final prompt with country-specific and language-specific instructions
            val finalPrompt = if (country != null || language != null) {
                val promptBuilder = StringBuilder()
                promptBuilder.append(systemPrompt)
                promptBuilder.append("\n\n")

                // Add country-specific instructions if country is provided
                if (country != null) {
                    // Map country code to full name if it's a code
                    val countryName = getCountryNameByCode(country)

                    promptBuilder.append("Пользователь находится в стране: $countryName.\n")
                    promptBuilder.append("Адаптируй свой ответ к правовой системе и законодательству этой страны.\n")
                    promptBuilder.append("Используй актуальные законы, нормативные акты и правовые документы этой страны.\n")
                    promptBuilder.append("Если цитируешь законы, конституцию или другие правовые документы, указывай их точные названия и номера статей.\n\n")
                }

                // Add language-specific instructions if language is provided
                if (language != null) {
                    val languageName = when (language) {
                        "ru" -> "русском"
                        "kk" -> "казахском"
                        "en" -> "английском"
                        else -> "русском" // Default to Russian if unknown language
                    }

                    promptBuilder.append("Пользователь предпочитает общение на $languageName языке.\n")
                    promptBuilder.append("Пожалуйста, отвечай на $languageName языке.\n\n")
                }

                promptBuilder.append("Вот мой вопрос: $userMessage")
                promptBuilder.toString()
            } else {
                """
                $systemPrompt

                Вот мой вопрос: $userMessage
                """
            }

            logger.info("Sending prompt to AI for chat ${chat.id}, country: ${country ?: "not specified"}, language: ${language ?: "not specified"}")
            val startTime = System.currentTimeMillis()

            val aiContent = try {
                chatClient.prompt(finalPrompt).call().content()
            } catch (e: Exception) {
                logger.error("Error calling AI service: ${e.message}", e)
                Translations.get("error", language)
            }

            val endTime = System.currentTimeMillis()
            logger.info("AI response received in ${endTime - startTime}ms for chat ${chat.id}")

            // Clean the response (remove hashtag headers)
            val cleanedContent = (aiContent ?: "").replace(Regex("###\\s*"), "")

            chatMemory.add(memoryId, listOf(AssistantMessage(cleanedContent)))

            val message =
                Message(
                    chat = chat,
                    content = cleanedContent,
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

    // Single-threaded executor for processing streaming responses
    private val executor = Executors.newSingleThreadExecutor()

    /**
     * Create a message and stream the AI response using Server-Sent Events (SSE)
     * This method handles the entire process of creating a message and streaming the response
     */
    fun createMessageStream(
        messageDTO: MessageDTO,
        userEmail: String
    ): SseEmitter {
        val chat =
            messageDTO.chatId?.let { chatService.getChatById(it) }
                ?: chatService.createChat(userService.getUserByEmail(userEmail), language = messageDTO.language)

        val message =
            Message(
                chat = chat,
                content = messageDTO.content,
                role = ChatMessageRole.user
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

        return askAIStream(messageDTO.content, chat, messageDTO.country, messageDTO.language)
    }

    /**
     * Stream AI responses using Server-Sent Events (SSE)
     * This method creates an SSE emitter and streams the AI response in chunks
     * to improve user experience and avoid timeouts
     *
     * Note: Due to an issue with the Spring AI streaming API (NoSuchMethodException for content()),
     * this implementation currently uses a workaround that gets the complete response first
     * and then sends it all at once. This is not true streaming, but it avoids the error.
     *
     * TODO: When the Spring AI library is updated or a proper streaming solution is available,
     * modify this method to use true streaming for better performance and user experience.
     */
    private fun askAIStream(
        userMessage: String,
        chat: Chat,
        country: String? = null,
        language: String? = null
    ): SseEmitter {
        val memoryId = chat.id.toString()
        val logger = org.slf4j.LoggerFactory.getLogger(MessageService::class.java)
        val emitter = SseEmitter(180000L) // 3 minute timeout

        // Create a placeholder for the assistant's response
        val assistantMessage = Message(
            chat = chat,
            content = "",
            role = ChatMessageRole.assistant
        )
        chat.messages.add(assistantMessage)
        val savedAssistantMessage = messageRepository.save(assistantMessage)

        // Add user message to chat memory
        chatMemory.add(memoryId, listOf(UserMessage(userMessage)))

        // Create final prompt with country-specific and language-specific instructions
        val finalPrompt = if (country != null || language != null) {
            val promptBuilder = StringBuilder()
            promptBuilder.append(systemPrompt)
            promptBuilder.append("\n\n")

            // Add country-specific instructions if country is provided
            if (country != null) {
                // Map country code to full name if it's a code
                val countryName = getCountryNameByCode(country)

                promptBuilder.append("Пользователь находится в стране: $countryName.\n")
                promptBuilder.append("Адаптируй свой ответ к правовой системе и законодательству этой страны.\n")
                promptBuilder.append("Используй актуальные законы, нормативные акты и правовые документы этой страны.\n")
                promptBuilder.append("Если цитируешь законы, конституцию или другие правовые документы, указывай их точные названия и номера статей.\n\n")
            }

            // Add language-specific instructions if language is provided
            if (language != null) {
                val languageName = when (language) {
                    "ru" -> "русском"
                    "kk" -> "казахском"
                    "en" -> "английском"
                    else -> "русском" // Default to Russian if unknown language
                }

                promptBuilder.append("Пользователь предпочитает общение на $languageName языке.\n")
                promptBuilder.append("Пожалуйста, отвечай на $languageName языке.\n\n")
            }

            promptBuilder.append("Вот мой вопрос: $userMessage")
            promptBuilder.toString()
        } else {
            """
            $systemPrompt

            Вот мой вопрос: $userMessage
            """
        }

        // Set up completion callback
        emitter.onCompletion {
            logger.info("SSE completed for chat ${chat.id}")
        }

        // Set up timeout callback
        emitter.onTimeout {
            logger.info("SSE timed out for chat ${chat.id}")
            try {
                // Send a timeout event to the client
                emitter.send(
                    SseEmitter.event()
                        .id("timeout")
                        .data("Request timed out", MediaType.TEXT_PLAIN)
                )
            } catch (e: Exception) {
                logger.error("Error sending timeout event: ${e.message}", e)
            } finally {
                // Always complete the emitter on timeout
                try {
                    emitter.complete()
                } catch (e: Exception) {
                    logger.error("Error completing emitter on timeout: ${e.message}", e)
                }
            }
        }

        // Set up error callback
        emitter.onError { error ->
            logger.error("SSE error for chat ${chat.id}: ${error.message}", error)
            try {
                // Send an error event to the client
                emitter.send(
                    SseEmitter.event()
                        .id("error")
                        .data("An error occurred: ${error.message}", MediaType.TEXT_PLAIN)
                )
            } catch (e: Exception) {
                logger.error("Error sending error event in onError callback: ${e.message}", e)
            } finally {
                // Always complete the emitter on error
                try {
                    emitter.complete()
                } catch (e: Exception) {
                    logger.error("Error completing emitter on error: ${e.message}", e)
                }
            }
        }

        // Process the streaming response asynchronously
        executor.execute {
            logger.info("Starting streaming response for chat ${chat.id}")
            val startTime = System.currentTimeMillis()

            try {
                // StringBuilder to accumulate the complete response
                val responseBuilder = StringBuilder()

                // Get the complete response first (non-streaming)
                // This is a workaround for the streaming API issue
                val completeContent = chatClient.prompt(finalPrompt).call().content()

                // Process the response
                try {
                    // Since we can't use streaming directly due to the API issue,
                    // we'll simulate streaming by breaking the complete content into smaller chunks
                    val content = completeContent

                    if (content != null && content.isNotEmpty()) {
                        // Break the content into paragraphs or smaller chunks
                        val chunks = content.split("\n\n")

                        // Send each chunk as a separate SSE event
                        for (chunk in chunks) {
                            if (chunk.isNotEmpty()) {
                                try {
                                    // Send the chunk to the client
                                    emitter.send(
                                        SseEmitter.event()
                                            .id(savedAssistantMessage.id.toString())
                                            .data(chunk, MediaType.TEXT_PLAIN)
                                    )

                                    // Small delay to prevent overwhelming the client
                                    Thread.sleep(50)

                                    // Add to the complete response
                                    responseBuilder.append(chunk).append("\n\n")
                                } catch (e: Exception) {
                                    logger.error("Error sending chunk: ${e.message}", e)
                                    break
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    logger.error("Error processing response: ${e.message}", e)
                }

                // Get the complete response
                val completeResponse = responseBuilder.toString()

                // Clean the response (remove hashtag headers)
                val cleanedContent = completeResponse.replace(Regex("###\\s*"), "")

                // Update the assistant message in the database with the complete response
                savedAssistantMessage.content = cleanedContent
                messageRepository.save(savedAssistantMessage)

                // Add to chat memory
                chatMemory.add(memoryId, listOf(AssistantMessage(cleanedContent)))

                val endTime = System.currentTimeMillis()
                logger.info("Streaming completed in ${endTime - startTime}ms for chat ${chat.id}")

                // Send a completion event
                try {
                    // Ensure there's a small delay before sending the completion event
                    // This helps ensure all content chunks are processed by the client
                    Thread.sleep(100)

                    emitter.send(
                        SseEmitter.event()
                            .id("complete")
                            .data("complete", MediaType.TEXT_PLAIN)
                    )

                    // Another small delay before completing the emitter
                    Thread.sleep(50)

                    // Complete the emitter
                    emitter.complete()
                } catch (e: Exception) {
                    logger.error("Error sending completion event: ${e.message}", e)
                    try {
                        emitter.complete()
                    } catch (completeEx: Exception) {
                        logger.error("Error completing emitter: ${completeEx.message}", completeEx)
                    }
                }

            } catch (e: Exception) {
                logger.error("Error in streaming response: ${e.message}", e)

                // Update the assistant message with an error
                val errorMessage = Translations.get("error", language)
                savedAssistantMessage.content = errorMessage
                messageRepository.save(savedAssistantMessage)

                try {
                    // Send error event
                    emitter.send(
                        SseEmitter.event()
                            .id("error")
                            .data(errorMessage, MediaType.TEXT_PLAIN)
                    )
                } catch (emitterEx: Exception) {
                    logger.error("Error sending error event: ${emitterEx.message}", emitterEx)
                } finally {
                    // Always complete the emitter, even if sending the error event fails
                    try {
                        emitter.complete()
                    } catch (completeEx: Exception) {
                        logger.error("Error completing emitter after error: ${completeEx.message}", completeEx)
                    }
                }
            }
        }

        return emitter
    }
}
