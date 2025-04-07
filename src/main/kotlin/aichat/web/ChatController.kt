package aichat.web

import org.springframework.ai.chat.client.ChatClient
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("api/chat")
class ChatController(chatClientBuilder: ChatClient.Builder) {

    private val chatClient: ChatClient = chatClientBuilder.build()
    private val systemPrompt = """
        You are a highly qualified and experienced legal expert with in-depth knowledge of international, national, and local laws across all jurisdictions. 
        Your task is to answer legal questions as accurately, clearly, and concisely as possible, while adapting your explanations to the user's language and region. 
        If a question requires jurisdiction-specific context, always ask for clarification or state which jurisdiction your answer applies to. 
        Be professional, neutral, and informative. Respond in the language the user uses.
    """.trimIndent()


    @GetMapping("/ai")
    fun generation(@RequestParam userInput: String): String? {
        return chatClient.prompt()
            .system(systemPrompt)
            .user(userInput)
            .call()
            .content()
    }
}
