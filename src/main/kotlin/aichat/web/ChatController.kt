package aichat.web

import org.springframework.ai.chat.client.ChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("api/chat")
class ChatController(
    chatClientBuilder: ChatClient.Builder,
    @Value("classpath:prompts/law-ai.st")
    private val lawAIPrompt: String
) {


    private val chatClient: ChatClient = chatClientBuilder.build()


    @GetMapping("/ai")
    fun generation(@RequestParam userInput: String): String? {
        return chatClient.prompt()
            .system(lawAIPrompt)
            .user(userInput)
            .call()
            .content().also {
                println("User: $userInput")
                println("AI: ${it}")
            }
    }
    
}
