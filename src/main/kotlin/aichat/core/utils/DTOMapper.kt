package aichat.core.utils

import aichat.core.dto.ChatRespDTO
import aichat.core.models.Message
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

/**
 * Utility class for mapping domain models to DTOs
 */
class DTOMapper {
    companion object {
        /**
         * Maps a Message entity to a ChatRespDTO
         * 
         * @param message The message entity to map
         * @return The mapped ChatRespDTO
         */
        fun mapMessageToDTO(message: Message): ChatRespDTO {
            val document = message.document
            val image = message.image
            val baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString()

            return ChatRespDTO(
                id = message.id,
                role = message.role,
                content = message.content,
                createdAt = message.createdAt,
                chatId = message.chat.id,
                documentId = document?.id,
                documentName = document?.name,
                documentUrl = document?.let { "$baseUrl/api/documents/${it.id}" },
                imageId = image?.id,
                imageName = image?.name,
                imageUrl = image?.let { "$baseUrl/api/images/${it.id}" },
                imageAlt = image?.name
            )
        }

        /**
         * Maps a list of Message entities to a list of ChatRespDTOs
         * 
         * @param messages The list of message entities to map
         * @return The list of mapped ChatRespDTOs
         */
        fun mapMessagesToDTO(messages: List<Message>): List<ChatRespDTO> {
            return messages.map { mapMessageToDTO(it) }
        }
    }
}