package com.example.app.api.model

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.Pattern

/**
 *
 * @param message The message to send to the AI agent
 * @param chatRequestId Chat Request identifier\\
 * @param chatSessionId Chat Session identifier  for the conversation
 * @param streaming
 */
data class ChatRequest(
    @get:JsonProperty("message", required = true) val message: String,
    @get:JsonProperty("chatRequestId") val chatRequestId: String? = null,
    // @get:Pattern(regexp = "^[_0-9a-f-]{16,64}$")
    @get:JsonProperty("chatSessionId") val chatSessionId: String? = null,
    @get:JsonProperty("streaming") val streaming: Boolean? = false,
)