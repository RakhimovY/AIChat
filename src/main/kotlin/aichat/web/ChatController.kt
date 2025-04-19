package aichat.web

import aichat.core.dto.ChatRespDTO
import aichat.core.dto.MessageDTO
import aichat.core.services.MessageService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.security.Principal


@RestController
@RequestMapping("api/chat")
class ChatController(
    private val messageService: MessageService,
) {
    @PostMapping("/ask")
    fun chat(@RequestBody messageDTO: MessageDTO, principal: Principal): List<ChatRespDTO> {
        return messageService.createMessage(messageDTO, principal.name)
    }
}
