package aichat.web

import aichat.core.dto.ChatDTO
import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.services.ChatService
import aichat.core.services.DocumentService
import aichat.core.services.ImageService
import aichat.core.services.MessageService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.security.Principal


@RestController
@RequestMapping("api/chat")
class ChatController(
    private val messageService: MessageService,
    private val chatService: ChatService,
    private val imageService: ImageService,
    private val documentService: DocumentService
) {
    @PostMapping(
        "/ask",
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.MULTIPART_FORM_DATA_VALUE, "multipart/form-data;boundary=*;charset=UTF-8"]
    )
    fun chat(
        principal: Principal,
        @RequestParam(value = "content", required = false) contentParam: String? = null,
        @RequestParam(value = "chatId", required = false) chatIdParam: Long? = null,
        @RequestParam(value = "country", required = false) countryParam: String? = null,
        @RequestParam(value = "language", required = false) languageParam: String? = null,
        @RequestParam(value = "document", required = false) documentParam: MultipartFile? = null,
        @RequestParam(value = "image", required = false) imageParam: MultipartFile? = null,
        @RequestBody(required = false) messageDTO: MessageDTO? = null
    ): List<ChatRespDTO> {
        val finalMessageDTO = if (messageDTO != null) {
            messageDTO
        } else {
            val content = contentParam ?: throw IllegalArgumentException("Content is required")
            if (documentParam != null && !documentParam.isEmpty) {
                if (!documentService.isValidDocumentType(documentParam)) {
                    throw IllegalArgumentException("Unsupported document type. Only PDF, Word, Excel, PowerPoint, text, and CSV files are supported.")
                }
                if (!documentService.isValidFileSize(documentParam)) {
                    throw IllegalArgumentException("Document size exceeds the maximum allowed size (10MB).")
                }
            }
            if (imageParam != null && !imageParam.isEmpty) {
                if (!imageService.isValidImageType(imageParam)) {
                    throw IllegalArgumentException("Unsupported image type. Only JPG, PNG, and WebP are supported.")
                }
                if (!imageService.isValidFileSize(imageParam)) {
                    throw IllegalArgumentException("Image size exceeds the maximum allowed size (50MB).")
                }
            }

            MessageDTO(
                chatId = chatIdParam,
                content = content,
                country = countryParam,
                language = languageParam,
                document = documentParam,
                image = imageParam
            )
        }

        return messageService.createMessage(finalMessageDTO, principal.name)
    }

    @GetMapping("/user")
    fun getUserChats(principal: Principal): List<ChatDTO> {
        return chatService.getUserChatsAsDTO(principal.name)
    }

    @GetMapping("/{chatId}")
    fun getChatMessages(@PathVariable chatId: Long): List<ChatRespDTO> {
        return messageService.getChatMessages(chatId)
    }

    @DeleteMapping("/{chatId}")
    fun deleteChat(@PathVariable chatId: Long): ResponseEntity<Void> {
        chatService.deleteChat(chatId)
        return ResponseEntity.ok().build()
    }

}
