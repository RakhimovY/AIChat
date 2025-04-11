package aichat.web

import aichat.core.dto.MessageDTO
import aichat.core.modles.Message
import aichat.core.services.MessageService
import org.springframework.web.bind.annotation.*
import java.security.Principal


@RestController
@RequestMapping("api/chat")
class ChatController(
    private val messageService: MessageService,
) {
    @GetMapping("/ai")
    fun generation(@RequestParam userInput: String): String? {
        return userInput
    }

    @PostMapping("/ask")
    fun chat(@RequestBody messageDTO: MessageDTO, principal: Principal): Message {
        return messageService.createMessage(messageDTO, principal.name)
    }
}
