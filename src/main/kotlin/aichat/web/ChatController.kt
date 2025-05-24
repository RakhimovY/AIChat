package aichat.web

import aichat.core.dto.ChatDTO
import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.services.ChatService
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
    private val chatService: ChatService
) {
    @PostMapping("/ask")
    fun chat(@RequestBody messageDTO: MessageDTO, principal: Principal): List<ChatRespDTO> {
        return messageService.createMessage(messageDTO, principal.name)
    }

    @PostMapping("/ask-with-document", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun chatWithDocument(
        @RequestParam("content") content: String,
        @RequestParam("document") document: MultipartFile,
        @RequestParam("chatId", required = false) chatId: Long?,
        @RequestParam("country", required = false) country: String?,
        @RequestParam("language", required = false) language: String?,
        principal: Principal
    ): List<ChatRespDTO> {
        // Create MessageDTO from form data
        val messageDTO = MessageDTO(
            chatId = chatId,
            content = content,
            country = country,
            language = language,
            document = document
        )
        return messageService.createMessage(messageDTO, principal.name)
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
