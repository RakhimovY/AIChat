package aichat.web

import aichat.core.dto.ChatDTO
import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.services.ChatService
import aichat.core.services.MessageService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
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
