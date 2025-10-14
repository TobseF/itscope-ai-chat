package de.itscope.ai.mcp.chat

import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.google.GoogleModels.Gemini2_5Flash
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.llm.LLModel
import de.itscope.ai.mcp.util.logging.Klogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/simple-chat")
class SimpleChatController(
    private val googleExecutor: SingleLLMPromptExecutor?,
) : Klogging {
    private val model: LLModel = Gemini2_5Flash

    @PostMapping
    suspend fun chat(
        @RequestBody request: ChatRequest,
    ): ResponseEntity<ChatResponse> =
        if (googleExecutor != null) {
            try {
                val prompt =
                    prompt("Der Kunde ein Produkt bestellt oder sucht") {
                        system(
                            "Du bist ein Berater dem dem Kunden hilft Produkte zu bestellen," +
                                " oder Produktinformationen zu recherchieren.",
                        )
                        user(request.message)
                    }

                val result = googleExecutor.execute(prompt, model)
                val responseText = result.joinToString(separator = System.lineSeparator()) { it.content }
                ResponseEntity.ok(ChatResponse(responseText))
            } catch (e: Exception) {
                log.error("Failed to execute chat", e)
                ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ChatResponse("Error processing request"))
            }
        } else {
            ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ChatResponse("AI service not configured"))
        }
}

data class ChatRequest(
    val message: String,
)

data class ChatResponse(
    val response: String,
)