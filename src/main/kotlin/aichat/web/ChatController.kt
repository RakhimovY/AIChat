package aichat.web

import aichat.core.services.ChatService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("api/chat")
class ChatController(
    private val chatService: ChatService,
) {
    @GetMapping("/ai")
    fun generation(@RequestParam userInput: String): String? {
        return userInput
    }
}
